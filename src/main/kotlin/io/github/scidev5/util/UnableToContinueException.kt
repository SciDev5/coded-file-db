package io.github.scidev5.util

class UnableToContinueException(val reason:String) : Exception ("unable to continue with execution, aborting")