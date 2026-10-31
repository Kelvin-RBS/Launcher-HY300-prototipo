// Conversor de taxa de áudio (estéreo, 16 bits) com interpolação cúbica (Catmull-Rom).
// Serve para entregar ao Android o áudio na taxa nativa do aparelho (normalmente 48000 Hz):
// com outra taxa (o SNES usa ~32040 Hz) o sistema precisa converter por conta própria e perde
// o caminho de baixa latência.
#pragma once
#include <stdint.h>
#include <stddef.h>
#include <vector>

class Resampler {
public:
    // inRate = taxa do núcleo, outRate = taxa do aparelho. Se forem iguais, só copia.
    void setup(double inRate, double outRate) {
        step_ = (inRate > 0.0 && outRate > 0.0) ? inRate / outRate : 1.0;
        passthrough_ = (inRate == outRate) || inRate <= 0.0 || outRate <= 0.0;
        reset();
    }

    void reset() {
        ext_.assign(3 * 2, 0);  // 3 quadros de histórico (silêncio)
        pos_ = 1.0;             // posição entre ext[1] e ext[2] (índices em quadros)
    }

    // Converte [in] (quadros estéreo intercalados) e escreve em [out] (capacidade outCap em
    // amostras, ou seja, quadros*2). Devolve quantas amostras (shorts) foram escritas.
    size_t process(const int16_t *in, size_t inFrames, int16_t *out, size_t outCap) {
        if (passthrough_) {
            size_t n = inFrames * 2;
            if (n > outCap) n = outCap;
            for (size_t i = 0; i < n; i++) out[i] = in[i];
            return n;
        }
        size_t histFrames = ext_.size() / 2;
        ext_.resize((histFrames + inFrames) * 2);
        for (size_t i = 0; i < inFrames * 2; i++) ext_[histFrames * 2 + i] = in[i];
        size_t total = histFrames + inFrames;

        size_t w = 0;
        // Precisa de ext[i-1], ext[i], ext[i+1], ext[i+2]
        while (true) {
            size_t i = (size_t)pos_;
            if (i + 2 >= total) break;
            if (w + 2 > outCap) break;
            double t = pos_ - (double)i;
            for (int c = 0; c < 2; c++) {
                double p0 = ext_[(i - 1) * 2 + c];
                double p1 = ext_[i * 2 + c];
                double p2 = ext_[(i + 1) * 2 + c];
                double p3 = ext_[(i + 2) * 2 + c];
                double v = p1 + 0.5 * t * (p2 - p0 + t * (2.0 * p0 - 5.0 * p1 + 4.0 * p2 - p3 + t * (3.0 * (p1 - p2) + p3 - p0)));
                if (v > 32767.0) v = 32767.0;
                if (v < -32768.0) v = -32768.0;
                out[w++] = (int16_t)(v + (v >= 0 ? 0.5 : -0.5));
            }
            pos_ += step_;
        }
        // Guarda os 3 últimos quadros como histórico para o próximo bloco
        size_t keep = 3;
        size_t drop = total - keep;
        pos_ -= (double)drop;
        std::vector<int16_t> tail(ext_.begin() + drop * 2, ext_.end());
        ext_.swap(tail);
        return w;
    }

private:
    double step_ = 1.0;
    double pos_ = 1.0;
    bool passthrough_ = true;
    std::vector<int16_t> ext_;
};
