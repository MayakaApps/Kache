package com.mayakapps.kache

expect fun <K : Any, V : Any> getMapByStrategy(strategy: KacheStrategy): MutableMap<K, V>