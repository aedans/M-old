import com.beust.kobalt.plugin.application.application
import com.beust.kobalt.plugin.packaging.assemble
import com.beust.kobalt.project

val m = project {
    name = "M"
    group = "com.aedans"
    artifactId = "m"
    version = "0.13"

    dependencies {
        compile("org.jetbrains.kotlin:kotlin-runtime:1.1.2")
        compile("org.jetbrains.kotlin:kotlin-stdlib:1.1.2")
        compile("org.jetbrains.kotlin:kotlin-reflect:1.1.2")
    }

    assemble {
        jar {
            fatJar = true
        }
    }

    application {
        taskName = "run"
        mainClass = "com.aedans.m.CliKt"
        args("main.m")
    }

    application {
        taskName = "tst"
        mainClass = "com.aedans.test.MTestKt"
    }
}
