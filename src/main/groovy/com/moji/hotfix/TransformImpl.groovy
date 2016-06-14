//package com.moji.hotfix
//
//import com.android.build.api.transform.*
//import com.android.build.gradle.internal.pipeline.TransformManager
//import org.apache.commons.codec.digest.DigestUtils
//import org.apache.commons.io.FileUtils
//import org.gradle.api.Project
//
//class TransformImpl extends Transform {
//    private final Project project
//    HashSet<String> includePackage
//    HashSet<String> excludeClass
//    String lastHotfixDir;
//    def debugOn
//    def patchList = []
//    def beforeDexTasks = []
//    private static final String NUWA_DIR = "NuwaDir"
//    private static final String NUWA_PATCHES = "nuwaPatches"
//
//    private static final String MAPPING_TXT = "mapping.txt"
//    private static final String HASH_TXT = "hash.txt"
//
//    private static final String DEBUG = "debug"
//
//    public TransformImpl(Project project) {
//        this.project = project
//    }
//
//    @Override
//    String getName() {
//        return "hotfix"
//    }
//
//    @Override
//    Set<QualifiedContent.ContentType> getInputTypes() {
//        return TransformManager.CONTENT_CLASS;
//    }
//
//
//    @Override
//    Set<QualifiedContent.Scope> getScopes() {
//        return TransformManager.SCOPE_FULL_PROJECT;
//    }
//
//
//    @Override
//    boolean isIncremental() {
//        return false;
//    }
//
//
//    @Override
//    void transform(Context context, Collection<TransformInput> inputs, Collection<TransformInput> referencedInputs, TransformOutputProvider outputProvider, boolean isIncremental) throws IOException, TransformException, InterruptedException {
//        initExtention()
//
//        /**
//         * 一些列变量定义
//         */
//        String buildAndFlavor = context.path.split("transformClassesWithHotpatchFor")[1];
//        File hotfixDir = new File("${project.buildDir}/outputs/hotfix")
////        project.logger.error "======buildAndFlavor======$buildAndFlavor"
//        def outputDir = new File("${hotfixDir}/${buildAndFlavor}")
//        def destHashFile = new File(outputDir, "${HASH_TXT}")
//        def destMapFile = new File("${hotfixDir}/${buildAndFlavor}/${MAPPING_TXT}");
//        def destPatchJarFile = new File("${hotfixDir}/${buildAndFlavor}/patch/${PATCH_FILE_NAME}");
//        def patchDir = new File("${context.temporaryDir.getParent()}/patch/")
//
//
//
//        /**
//         * 遍历输入文件
//         */
//        inputs.each { TransformInput input ->
//            /**
//             * 遍历目录
//             */
//            input.directoryInputs.each { DirectoryInput directoryInput ->
//                /**
//                 * 获得产物的目录
//                 */
//                File dest = outputProvider.getContentLocation(directoryInput.name, directoryInput.contentTypes, directoryInput.scopes, Format.DIRECTORY);
//                String buildTypes = directoryInput.file.name
//                String productFlavors = directoryInput.file.parentFile.name
//                //这里进行我们的处理 TODO
//                project.logger.error "Copying ${directoryInput.name} to ${dest.absolutePath}"
//                /**
//                 * 处理完后拷到目标文件
//                 */
//                FileUtils.copyDirectory(directoryInput.file, dest);
//            }
//
//            /**
//             * 遍历jar
//             */
//            input.jarInputs.each { JarInput jarInput ->
//                String destName = jarInput.name;
//                /**
//                 * 重名名输出文件,因为可能同名,会覆盖
//                 */
//                def hexName = DigestUtils.md5Hex(jarInput.file.absolutePath);
//                if (destName.endsWith(".jar")) {
//                    destName = destName.substring(0, destName.length() - 4);
//                }
//                /**
//                 * 获得输出文件
//                 */
//                File dest = outputProvider.getContentLocation(destName + "_" + hexName, jarInput.contentTypes, jarInput.scopes, Format.JAR);
//
//                //处理jar进行字节码注入处理TODO
//
//                FileUtils.copyFile(jarInput.file, dest);
//                project.logger.error "Copying ${jarInput.file.absolutePath} to ${dest.absolutePath}"
//            }
//        }
//    }
//
//    private void initExtention() {
//        def extension = project.extensions.findByName("hotfix") as HotfixExtension
//        includePackage = extension.includePackage
//        excludeClass = extension.excludeClass
//        lastHotfixDir = extension.lastHotfixDir
//        debugOn = extension.debugOn
//    }
//
//}