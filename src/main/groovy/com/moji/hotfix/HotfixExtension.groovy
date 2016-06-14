package com.moji.hotfix

import org.gradle.api.Project

/**
 * Created by yilong.zhang on 16/5/21.
 */
class HotfixExtension {
    HashSet<String> includePackage = []
    HashSet<String> excludeClass = []
    HashSet<String> excludePackage = []
    boolean debugOn = true
    boolean isFix = false

    HotfixExtension(Project project) {
    }
}
