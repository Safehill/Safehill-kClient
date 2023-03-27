import java.util.Base64

fun main(args: Array<String>) {
    val string = "Hello World!"
    println(string)

    // Try adding program arguments via Run/Debug configuration.
    // Learn more about running applications: https://www.jetbrains.com/help/idea/running-applications.html.
    println("Program arguments: ${args.joinToString()}")

    val base64Encoded = Base64.getEncoder().encode(string.toByteArray())
    println(base64Encoded)
}