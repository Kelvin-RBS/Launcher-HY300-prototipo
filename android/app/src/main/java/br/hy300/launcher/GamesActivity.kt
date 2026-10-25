package br.hy300.launcher

import android.app.Activity
import android.content.ComponentName
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Color
import android.graphics.drawable.GradientDrawable
import android.os.Bundle
import android.os.Environment
import android.view.View
import android.view.ViewGroup
import android.widget.BaseAdapter
import android.widget.LinearLayout
import android.widget.ListView
import android.widget.TextView
import android.widget.Toast
import java.io.File

/** Um sistema de jogos: pasta, núcleo do RetroArch e extensões aceitas. */
class GameSystem(val folder: String, val label: String, val coreFile: String, val extensions: List<String>)

class Game(val system: GameSystem, val file: File, val title: String)

object Games {
    /** Pasta dos jogos no projetor. Dentro dela: uma subpasta por sistema (snes, megadrive). */
    const val ROOT = "/storage/emulated/0/Jogos"

    const val RETROARCH_ACTIVITY = "com.retroarch.browser.retroactivity.RetroActivityFuture"

    /** O aparelho é de 32 bits, então o primeiro é a versão certa (ra32). */
    val RETROARCH_PACKAGES = listOf("com.retroarch.ra32", "com.retroarch", "com.retroarch.aarch64")

    val SYSTEMS = listOf(
        GameSystem("snes", "Super Nintendo", "snes9x_libretro_android.so", listOf("sfc", "smc", "swc", "fig")),
        GameSystem("megadrive", "Mega Drive", "genesis_plus_gx_libretro_android.so", listOf("md", "gen", "smd", "bin"))
    )
}

/** Lista os jogos das pastas e abre o escolhido no RetroArch. */
class GamesActivity : Activity() {

    private var games: List<Game> = emptyList()
    private lateinit var adapter: BaseAdapter
    private lateinit var status: TextView
    private lateinit var list: ListView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val root = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setBackgroundColor(Color.parseColor("#060A14"))
            setPadding(dp(43), dp(24), dp(43), dp(16))
        }
        root.addView(TextView(this).apply {
            text = "Jogos"
            textSize = 28f
            setTextColor(Color.parseColor("#EAF1FF"))
        })
        status = TextView(this).apply {
            textSize = 14f
            setTextColor(Color.parseColor("#A9B8D2"))
            setPadding(0, dp(4), 0, dp(12))
        }
        root.addView(status)

        list = ListView(this).apply {
            divider = null
            dividerHeight = 0
            selector = GradientDrawable().apply {
                cornerRadius = dp(8).toFloat()
                setColor(Color.argb(40, 227, 238, 255))
                setStroke(dp(2), Color.rgb(227, 238, 255))
            }
        }
        adapter = object : BaseAdapter() {
            override fun getCount() = games.size
            override fun getItem(position: Int) = games[position]
            override fun getItemId(position: Int) = position.toLong()
            override fun getView(position: Int, convertView: View?, parent: ViewGroup?): View {
                val g = games[position]
                val row = LinearLayout(this@GamesActivity).apply {
                    orientation = LinearLayout.VERTICAL
                    setPadding(dp(14), dp(8), dp(14), dp(8))
                }
                row.addView(TextView(this@GamesActivity).apply {
                    text = g.title
                    textSize = 18f
                    setTextColor(Color.WHITE)
                })
                row.addView(TextView(this@GamesActivity).apply {
                    text = g.system.label
                    textSize = 12f
                    setTextColor(Color.parseColor("#A9B8D2"))
                })
                return row
            }
        }
        list.adapter = adapter
        list.setOnItemClickListener { _, _, position, _ -> play(games[position]) }
        root.addView(list, LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, 0, 1f))
        setContentView(root)
    }

    override fun onResume() {
        super.onResume()
        refresh()
    }

    private fun refresh() {
        val retroArch = retroArchPackage()
        val canRead = Environment.isExternalStorageManager()
        games = if (canRead) findGames() else emptyList()
        adapter.notifyDataSetChanged()
        status.text = when {
            retroArch == null -> "RetroArch não encontrado. Instale o RetroArch de 32 bits (RetroArch_ra32.apk)."
            !canRead -> "Falta liberar o acesso a arquivos. No computador: adb shell appops set " + packageName + " MANAGE_EXTERNAL_STORAGE allow"
            games.isEmpty() -> "Nenhum jogo em " + Games.ROOT + " (subpastas snes e megadrive)."
            else -> games.size.toString() + " jogo(s)  \u00B7  OK para jogar  \u00B7  Voltar sai"
        }
        if (games.isNotEmpty()) list.requestFocus()
    }

    private fun findGames(): List<Game> {
        val result = ArrayList<Game>()
        for (system in Games.SYSTEMS) {
            val files = File(Games.ROOT, system.folder).listFiles() ?: continue
            for (f in files.sortedBy { it.name.lowercase() }) {
                if (f.isFile && system.extensions.contains(f.extension.lowercase())) {
                    result.add(Game(system, f, cleanTitle(f.nameWithoutExtension)))
                }
            }
        }
        return result
    }

    private fun cleanTitle(name: String): String =
        name.replace('_', ' ').split(" ").filter { it.isNotEmpty() }.joinToString(" ")

    private fun retroArchPackage(): String? {
        for (p in Games.RETROARCH_PACKAGES) {
            try {
                packageManager.getPackageInfo(p, 0)
                return p
            } catch (e: PackageManager.NameNotFoundException) {
                // tenta o próximo
            }
        }
        return null
    }

    private fun play(g: Game) {
        val pkg = retroArchPackage()
        if (pkg == null) {
            Toast.makeText(this, "RetroArch não encontrado", Toast.LENGTH_LONG).show()
            return
        }
        val intent = Intent(Intent.ACTION_MAIN)
            .setComponent(ComponentName(pkg, Games.RETROARCH_ACTIVITY))
            .putExtra("ROM", g.file.absolutePath)
            .putExtra("LIBRETRO", "/data/data/" + pkg + "/cores/" + g.system.coreFile)
            .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP)
        try {
            startActivity(intent)
        } catch (e: Exception) {
            Toast.makeText(this, "Não foi possível abrir o RetroArch", Toast.LENGTH_LONG).show()
        }
    }
}
