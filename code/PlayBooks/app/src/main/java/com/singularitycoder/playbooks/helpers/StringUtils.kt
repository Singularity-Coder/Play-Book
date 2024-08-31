package com.singularitycoder.playbooks.helpers

import java.util.Locale

fun String.toUpCase(): String = this.uppercase(Locale.getDefault())