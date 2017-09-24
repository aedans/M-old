import com.beust.kobalt.plugin.application.application
import com.beust.kobalt.plugin.packaging.assemble
import com.beust.kobalt.project

val m = project {
    name = "M"
    group = "io.github.aedans"
    artifactId = "m"
    version = "0.17"

    dependencies {
        compile("org.jetbrains.kotlin:kotlin-runtime:1.1.2")
        compile("org.jetbrains.kotlin:kotlin-stdlib:1.1.2")
        compile("org.jetbrains.kotlin:kotlin-reflect:1.1.2")
        compile("io.github.aedans:kotlin-cons:1.1.0")
    }

    assemble {
        jar {
            name = "M.jar"
            fatJar = true
            addAttribute("Main-Class", "com.aedans.m.CliKt")
        }
    }

    application {
        taskName = "run"
        mainClass = "io.github.aedans.m.CliKt"
        args("main.m")
    }

    application {
        taskName = "tst"
        mainClass = "io.github.aedans.test.MTestKt"
    }
}
