package com.moji.hotfix.util

/**
 * Created by yilong.zhang on 16/5/21.
 */
class HotfixSetUtils {
    public
    static boolean isExcluded(String path, Set<String> excludePackage, Set<String> excludeClass,Set<String> includePachage) {
        if(path.contains(File.separator)){
            path = path.replace(File.separator,"/")
        }
        for (String exclude:excludeClass){
            if(path.equals(exclude)) {
//                println("isExcluded:"+path+"return "+true)
                return  true;
            }
        }
        for (String exclude:excludePackage){
            if(path.contains(exclude)) {
//                println("isExcluded:"+path+"return "+true)
                return  true;
            }
        }
        println("isExcluded:"+path+"return "+false)
        if(!path.contains("com/moji/")&&!isIncluded(path,includePachage)) {
            return true;
        }
//        println("isExcluded:"+path+"return "+false)
        return false;
    }


    public static boolean isIncluded(String path, Set<String> includePackage) {
        if (includePackage.size() == 0) {
            return true
        }

        def isIncluded = false;
        includePackage.each { include ->
            if (path.contains(include)) {
                isIncluded = true
            }
        }
        return isIncluded
    }
}
