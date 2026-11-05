package dev.brua.android

/** Why a [BruaImage] has no picture. The same reasons on every platform's SDK. */
public sealed class BruaImageError {
    /** Nothing is released under the id: the edge answered 404. Usually a call site draws nothing. */
    public data object NotReleased : BruaImageError()

    /** The edge can't be reached and no release of the id is known. */
    public data class Unreachable(val cause: Throwable) : BruaImageError()

    /** The asset is known, but its file failed to load or decode. */
    public data class File(val url: String, val cause: Throwable) : BruaImageError()
}
