// Motor Snes9x HY300: carrega um núcleo libretro (Snes9x) e entrega vídeo, áudio e entrada ao app.
#include <jni.h>
#include <android/log.h>
#include <android/native_window.h>
#include <android/native_window_jni.h>
#include <dlfcn.h>
#include <stdint.h>
#include <stdlib.h>
#include <string.h>
#include <stdarg.h>
#include <stdio.h>
#include <time.h>
#include <mutex>
#include <string>
#include <vector>
#include "resample.h"

#define LOG_TAG "hy300emu"
#define LOGE(...) __android_log_print(ANDROID_LOG_ERROR, LOG_TAG, __VA_ARGS__)

// ---- tipos da API libretro (iguais aos do libretro.h) ----
struct retro_game_info { const char *path; const void *data; size_t size; const char *meta; };
struct retro_system_info { const char *library_name; const char *library_version; const char *valid_extensions; bool need_fullpath; bool block_extract; };
struct retro_game_geometry { unsigned base_width; unsigned base_height; unsigned max_width; unsigned max_height; float aspect_ratio; };
struct retro_system_timing { double fps; double sample_rate; };
struct retro_system_av_info { retro_game_geometry geometry; retro_system_timing timing; };

typedef bool (*retro_environment_t)(unsigned cmd, void *data);
typedef void (*retro_video_refresh_t)(const void *data, unsigned width, unsigned height, size_t pitch);
typedef void (*retro_audio_sample_t)(int16_t left, int16_t right);
typedef size_t (*retro_audio_sample_batch_t)(const int16_t *data, size_t frames);
typedef void (*retro_input_poll_t)(void);
typedef int16_t (*retro_input_state_t)(unsigned port, unsigned device, unsigned index, unsigned id);

// ---- funções do núcleo ----
static void *g_core = nullptr;
static void (*p_set_environment)(retro_environment_t);
static void (*p_set_video_refresh)(retro_video_refresh_t);
static void (*p_set_audio_sample)(retro_audio_sample_t);
static void (*p_set_audio_sample_batch)(retro_audio_sample_batch_t);
static void (*p_set_input_poll)(retro_input_poll_t);
static void (*p_set_input_state)(retro_input_state_t);
static void (*p_init)(void);
static void (*p_deinit)(void);
static void (*p_get_system_av_info)(retro_system_av_info *);
static bool (*p_load_game)(const retro_game_info *);
static void (*p_unload_game)(void);
static void (*p_run)(void);
static void (*p_reset)(void);
static size_t (*p_serialize_size)(void);
static bool (*p_serialize)(void *, size_t);
static bool (*p_unserialize)(const void *, size_t);
static void *(*p_get_memory_data)(unsigned);
static size_t (*p_get_memory_size)(unsigned);

// ---- estado ----
static std::mutex g_window_mutex;
static ANativeWindow *g_window = nullptr;
static int g_pixfmt = 2;  // 0 = 0RGB1555, 1 = XRGB8888, 2 = RGB565
static int g_w = 0, g_h = 0;
static volatile uint16_t g_input[2] = {0, 0};
static const size_t AUDIO_MAX = 8192;
static int16_t g_audio[AUDIO_MAX];
static size_t g_audio_len = 0;
static const size_t AUDIO_OUT_MAX = 16384;  // saída já convertida para a taxa do aparelho
static int16_t g_audio_out[AUDIO_OUT_MAX];
static Resampler g_resampler;
static std::vector<uint8_t> g_rom;
static std::string g_romname;
static std::string g_dir = ".";
static std::string g_err;
// ---- medições de vídeo (diagnóstico de atraso) ----
static double now_ms() {
    timespec ts;
    clock_gettime(CLOCK_MONOTONIC, &ts);
    return ts.tv_sec * 1000.0 + ts.tv_nsec / 1000000.0;
}
static double g_lockSum = 0, g_lockMax = 0, g_cbSum = 0, g_cbMax = 0;
static long g_vidN = 0;
static long g_dropped = 0;       // quadros descartados pela "fila de vídeo curta"
static volatile int g_trim = 0;  // 1 = descarta um quadro quando a fila de vídeo enche
static int g_skip = 0;
static int g_sinceDrop = 1000;  // quadros desde o último descarte (limita a 1 descarte a cada 30 quadros)

static std::string g_corelog;  // o que o núcleo disse (erros e avisos), para explicar uma recusa

struct retro_log_callback { void (*log)(int level, const char *fmt, ...); };
static void core_log(int level, const char *fmt, ...) {
    char line[512];
    va_list ap;
    va_start(ap, fmt);
    vsnprintf(line, sizeof(line), fmt, ap);
    va_end(ap);
    __android_log_print(level >= 3 ? ANDROID_LOG_ERROR : ANDROID_LOG_INFO, "hy300core", "%s", line);
    if (level >= 1) {  // 0 = depuração (ignora)
        g_corelog += line;
        if (g_corelog.size() > 1500) g_corelog.erase(0, g_corelog.size() - 1500);
    }
}

static bool env_cb(unsigned cmd, void *data) {
    switch (cmd & 0xFFFF) {
        case 3:  *(bool *)data = true; return true;                 // GET_CAN_DUPE
        case 9:  *(const char **)data = g_dir.c_str(); return true;  // GET_SYSTEM_DIRECTORY
        case 31: *(const char **)data = g_dir.c_str(); return true;  // GET_SAVE_DIRECTORY
        case 10: {                                                   // SET_PIXEL_FORMAT
            int f = *(int *)data;
            if (f == 0 || f == 1 || f == 2) { g_pixfmt = f; return true; }
            return false;
        }
        case 27: ((retro_log_callback *)data)->log = core_log; return true;  // GET_LOG_INTERFACE
        case 16: return true;                                        // SET_VARIABLES
        case 17: *(bool *)data = false; return true;                 // GET_VARIABLE_UPDATE
        case 11: return true;                                        // SET_INPUT_DESCRIPTORS
        case 18: return true;                                        // SET_SUPPORT_NO_GAME
        case 35: return true;                                        // SET_CONTROLLER_INFO
        case 37: return true;                                        // SET_GEOMETRY
        case 32: return true;                                        // SET_SYSTEM_AV_INFO
        case 8:  return true;                                        // SET_PERFORMANCE_LEVEL
        default: return false;  // inclui GET_VARIABLE (usa os valores padrão do núcleo)
    }
}

static void video_cb(const void *data, unsigned width, unsigned height, size_t pitch) {
    if (!data) return;  // quadro repetido
    std::lock_guard<std::mutex> lock(g_window_mutex);
    if (!g_window) return;
    g_sinceDrop++;
    if (g_trim && g_skip > 0) {  // deixa a fila de vídeo esvaziar um quadro
        g_skip--;
        g_dropped++;
        g_sinceDrop = 0;
        return;
    }
    double tStart = now_ms();
    if ((int)width != g_w || (int)height != g_h) {
        ANativeWindow_setBuffersGeometry(g_window, (int32_t)width, (int32_t)height, WINDOW_FORMAT_RGB_565);
        g_w = (int)width;
        g_h = (int)height;
    }
    ANativeWindow_Buffer buf;
    double tLock = now_ms();
    int lockRes = ANativeWindow_lock(g_window, &buf, nullptr);
    // Tempo esperando um buffer livre: alto = a fila de vídeo está cheia (atraso extra de imagem)
    double lockMs = now_ms() - tLock;
    if (g_trim && lockMs > 5.0 && g_sinceDrop >= 30) g_skip = 1;
    if (lockRes != 0) return;
    unsigned rows = height < (unsigned)buf.height ? height : (unsigned)buf.height;
    unsigned cols = width < (unsigned)buf.width ? width : (unsigned)buf.width;
    const uint8_t *src = (const uint8_t *)data;
    uint16_t *dst = (uint16_t *)buf.bits;
    for (unsigned y = 0; y < rows; y++) {
        uint16_t *out = dst + (size_t)y * buf.stride;
        const uint8_t *row = src + (size_t)y * pitch;
        if (g_pixfmt == 2) {
            memcpy(out, row, (size_t)cols * 2);
        } else if (g_pixfmt == 1) {
            const uint32_t *in = (const uint32_t *)row;
            for (unsigned x = 0; x < cols; x++) {
                uint32_t p = in[x];
                out[x] = (uint16_t)(((p >> 8) & 0xF800) | ((p >> 5) & 0x07E0) | ((p >> 3) & 0x001F));
            }
        } else {
            const uint16_t *in = (const uint16_t *)row;
            for (unsigned x = 0; x < cols; x++) {
                uint16_t p = in[x];
                uint16_t r = (p >> 10) & 31, g = (p >> 5) & 31, b = p & 31;
                out[x] = (uint16_t)((r << 11) | (((g << 1) | (g >> 4)) << 5) | b);
            }
        }
    }
    ANativeWindow_unlockAndPost(g_window);
    double cbMs = now_ms() - tStart;
    g_lockSum += lockMs;
    if (lockMs > g_lockMax) g_lockMax = lockMs;
    g_cbSum += cbMs;
    if (cbMs > g_cbMax) g_cbMax = cbMs;
    g_vidN++;
}

static size_t audio_batch_cb(const int16_t *data, size_t frames) {
    size_t n = frames * 2;
    if (g_audio_len + n > AUDIO_MAX) n = AUDIO_MAX - g_audio_len;
    memcpy(g_audio + g_audio_len, data, n * sizeof(int16_t));
    g_audio_len += n;
    return frames;
}

static void audio_cb(int16_t left, int16_t right) {
    if (g_audio_len + 2 <= AUDIO_MAX) {
        g_audio[g_audio_len++] = left;
        g_audio[g_audio_len++] = right;
    }
}

static void input_poll_cb(void) {}

static int16_t input_state_cb(unsigned port, unsigned device, unsigned index, unsigned id) {
    (void)index;
    if (port > 1 || device != 1 || id > 15) return 0;  // 1 = RETRO_DEVICE_JOYPAD
    return (int16_t)((g_input[port] >> id) & 1);
}

extern "C" {

JNIEXPORT jboolean JNICALL Java_br_hy300_games_NativeEmu_loadCore(JNIEnv *env, jobject, jstring jpath, jstring jdir) {
    if (g_core) { dlclose(g_core); g_core = nullptr; }
    const char *path = env->GetStringUTFChars(jpath, nullptr);
    const char *dir = env->GetStringUTFChars(jdir, nullptr);
    g_dir = dir;
    g_core = dlopen(path, RTLD_NOW | RTLD_LOCAL);
    if (!g_core) { const char *e = dlerror(); g_err = e ? e : "dlopen falhou"; LOGE("%s", g_err.c_str()); }
    env->ReleaseStringUTFChars(jpath, path);
    env->ReleaseStringUTFChars(jdir, dir);
    if (!g_core) return JNI_FALSE;

    bool ok = true;
#define SYM(var, name) *(void **)(&var) = dlsym(g_core, name); if (!var) { g_err = std::string("falta ") + name; LOGE("%s", g_err.c_str()); ok = false; }
    SYM(p_set_environment, "retro_set_environment")
    SYM(p_set_video_refresh, "retro_set_video_refresh")
    SYM(p_set_audio_sample, "retro_set_audio_sample")
    SYM(p_set_audio_sample_batch, "retro_set_audio_sample_batch")
    SYM(p_set_input_poll, "retro_set_input_poll")
    SYM(p_set_input_state, "retro_set_input_state")
    SYM(p_init, "retro_init")
    SYM(p_deinit, "retro_deinit")
    SYM(p_get_system_av_info, "retro_get_system_av_info")
    SYM(p_load_game, "retro_load_game")
    SYM(p_unload_game, "retro_unload_game")
    SYM(p_run, "retro_run")
    SYM(p_reset, "retro_reset")
    SYM(p_serialize_size, "retro_serialize_size")
    SYM(p_serialize, "retro_serialize")
    SYM(p_unserialize, "retro_unserialize")
    SYM(p_get_memory_data, "retro_get_memory_data")
    SYM(p_get_memory_size, "retro_get_memory_size")
#undef SYM
    if (!ok) { dlclose(g_core); g_core = nullptr; return JNI_FALSE; }

    p_set_environment(env_cb);
    p_init();
    p_set_video_refresh(video_cb);
    p_set_audio_sample(audio_cb);
    p_set_audio_sample_batch(audio_batch_cb);
    p_set_input_poll(input_poll_cb);
    p_set_input_state(input_state_cb);
    return JNI_TRUE;
}

JNIEXPORT jstring JNICALL Java_br_hy300_games_NativeEmu_lastError(JNIEnv *env, jobject) {
    return env->NewStringUTF(g_err.c_str());
}

JNIEXPORT jboolean JNICALL Java_br_hy300_games_NativeEmu_loadGame(JNIEnv *env, jobject, jbyteArray jdata, jstring jname) {
    if (!g_core) return JNI_FALSE;
    jsize len = env->GetArrayLength(jdata);
    g_rom.resize((size_t)len);
    env->GetByteArrayRegion(jdata, 0, len, (jbyte *)g_rom.data());
    const char *name = env->GetStringUTFChars(jname, nullptr);
    g_romname = name;
    env->ReleaseStringUTFChars(jname, name);
    retro_game_info info;
    info.path = g_romname.c_str();
    info.data = g_rom.data();
    info.size = g_rom.size();
    info.meta = nullptr;
    g_w = 0;
    g_h = 0;
    g_audio_len = 0;
    g_corelog.clear();
    return p_load_game(&info) ? JNI_TRUE : JNI_FALSE;
}

// [média da espera por buffer, máxima, média do tempo total do quadro de vídeo, máxima, quadros, descartados]
// e zera os acumuladores (chamar a cada segundo, da thread do emulador).
JNIEXPORT jdoubleArray JNICALL Java_br_hy300_games_NativeEmu_videoStats(JNIEnv *env, jobject) {
    long n = g_vidN;
    double v[6] = {n ? g_lockSum / n : 0.0, g_lockMax, n ? g_cbSum / n : 0.0, g_cbMax, (double)n, (double)g_dropped};
    g_lockSum = g_lockMax = g_cbSum = g_cbMax = 0;
    g_vidN = 0;
    jdoubleArray arr = env->NewDoubleArray(6);
    env->SetDoubleArrayRegion(arr, 0, 6, v);
    return arr;
}

JNIEXPORT void JNICALL Java_br_hy300_games_NativeEmu_setVideoTrim(JNIEnv *, jobject, jint on) {
    g_trim = on ? 1 : 0;
    g_skip = 0;
    g_dropped = 0;
    g_sinceDrop = 1000;
}

JNIEXPORT jstring JNICALL Java_br_hy300_games_NativeEmu_lastLog(JNIEnv *env, jobject) {
    return env->NewStringUTF(g_corelog.c_str());
}

JNIEXPORT jdoubleArray JNICALL Java_br_hy300_games_NativeEmu_avInfo(JNIEnv *env, jobject) {
    retro_system_av_info av;
    memset(&av, 0, sizeof(av));
    if (g_core) p_get_system_av_info(&av);
    double v[5] = {av.timing.fps, av.timing.sample_rate, (double)av.geometry.base_width,
                   (double)av.geometry.base_height, (double)av.geometry.aspect_ratio};
    jdoubleArray arr = env->NewDoubleArray(5);
    env->SetDoubleArrayRegion(arr, 0, 5, v);
    return arr;
}

JNIEXPORT void JNICALL Java_br_hy300_games_NativeEmu_setSurface(JNIEnv *env, jobject, jobject surface) {
    std::lock_guard<std::mutex> lock(g_window_mutex);
    if (g_window) { ANativeWindow_release(g_window); g_window = nullptr; }
    g_w = 0;
    g_h = 0;
    if (surface) g_window = ANativeWindow_fromSurface(env, surface);
}

JNIEXPORT jint JNICALL Java_br_hy300_games_NativeEmu_runFrame(JNIEnv *env, jobject, jshortArray out) {
    if (!g_core) return 0;
    g_audio_len = 0;
    p_run();
    jsize cap = env->GetArrayLength(out);
    // Converte para a taxa do aparelho (sem efeito se for a mesma taxa do núcleo)
    jsize n = (jsize)g_resampler.process(g_audio, g_audio_len / 2, g_audio_out, AUDIO_OUT_MAX);
    if (n > cap) n = cap;
    env->SetShortArrayRegion(out, 0, n, (const jshort *)g_audio_out);
    return n;
}

// Define a taxa do núcleo e a taxa de saída do aparelho (chamar depois de carregar o jogo).
JNIEXPORT void JNICALL Java_br_hy300_games_NativeEmu_setAudioRates(JNIEnv *, jobject, jdouble inRate, jdouble outRate) {
    g_resampler.setup(inRate, outRate);
}

JNIEXPORT void JNICALL Java_br_hy300_games_NativeEmu_setInput(JNIEnv *, jobject, jint port, jint mask) {
    if (port >= 0 && port < 2) g_input[port] = (uint16_t)mask;
}

JNIEXPORT jbyteArray JNICALL Java_br_hy300_games_NativeEmu_serializeState(JNIEnv *env, jobject) {
    if (!g_core) return nullptr;
    size_t size = p_serialize_size();
    if (size == 0) return nullptr;
    std::vector<uint8_t> buf(size);
    if (!p_serialize(buf.data(), size)) return nullptr;
    jbyteArray arr = env->NewByteArray((jsize)size);
    env->SetByteArrayRegion(arr, 0, (jsize)size, (const jbyte *)buf.data());
    return arr;
}

JNIEXPORT jboolean JNICALL Java_br_hy300_games_NativeEmu_unserializeState(JNIEnv *env, jobject, jbyteArray data) {
    if (!g_core) return JNI_FALSE;
    jsize len = env->GetArrayLength(data);
    std::vector<uint8_t> buf((size_t)len);
    env->GetByteArrayRegion(data, 0, len, (jbyte *)buf.data());
    return p_unserialize(buf.data(), buf.size()) ? JNI_TRUE : JNI_FALSE;
}

JNIEXPORT jbyteArray JNICALL Java_br_hy300_games_NativeEmu_sram(JNIEnv *env, jobject) {
    if (!g_core) return nullptr;
    void *mem = p_get_memory_data(0);  // 0 = RETRO_MEMORY_SAVE_RAM
    size_t size = p_get_memory_size(0);
    if (!mem || size == 0) return nullptr;
    jbyteArray arr = env->NewByteArray((jsize)size);
    env->SetByteArrayRegion(arr, 0, (jsize)size, (const jbyte *)mem);
    return arr;
}

JNIEXPORT void JNICALL Java_br_hy300_games_NativeEmu_setSram(JNIEnv *env, jobject, jbyteArray data) {
    if (!g_core) return;
    void *mem = p_get_memory_data(0);
    size_t size = p_get_memory_size(0);
    if (!mem || size == 0) return;
    jsize len = env->GetArrayLength(data);
    size_t n = (size_t)len < size ? (size_t)len : size;
    env->GetByteArrayRegion(data, 0, (jsize)n, (jbyte *)mem);
}

JNIEXPORT void JNICALL Java_br_hy300_games_NativeEmu_reset(JNIEnv *, jobject) {
    if (g_core) p_reset();
}

JNIEXPORT void JNICALL Java_br_hy300_games_NativeEmu_unload(JNIEnv *, jobject) {
    if (g_core) {
        p_unload_game();
        p_deinit();
        dlclose(g_core);
        g_core = nullptr;
    }
    g_rom.clear();
    g_rom.shrink_to_fit();
}

}  // extern "C"
