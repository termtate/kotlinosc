package io.github.termtate.kotlinosc.exception

public sealed class OscException(message: String, cause: Throwable? = null) : Exception(message, cause)

public open class OscCodecException(message: String, cause: Throwable? = null) : OscException(message, cause)

public class OscPacketIllegalBytesException(message: String) : OscException(message)

public open class OscTypeTagParseException(message: String) : OscCodecException(message)

public class OscBundleParseException(message: String) : OscCodecException(message)

public class OscArrayTagParseException(message: String) : OscTypeTagParseException(message)

public class OscUnknownTypeTagException(message: String = "Unexpected type tag content") : OscTypeTagParseException(message)

public class OscBufferUnderflowException(message: String) : OscCodecException(message)

public class OscAddressParseException(message: String) : OscCodecException(message)

internal class OscPacketSchedulerException(message: String) : OscException(message)

public class OscLifecycleException(message: String) : OscException(message)

public class OscTransportException(
    message: String,
    cause: Throwable? = null
) : OscException(message, cause)

public class OscDispatchException(
    message: String,
    cause: Throwable? = null
) : OscException(message, cause)

internal class OscFrameException(message: String) : OscException(message)
