package crux.bphc.cms.interfaces

fun interface ClickListener {
    fun onClick(`object`: Any, position: Int): Boolean
}