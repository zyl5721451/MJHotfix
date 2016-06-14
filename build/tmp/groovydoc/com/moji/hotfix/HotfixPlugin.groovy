package com.moji.hotfix

import com.moji.hotfix.util.HotfixAndroidUtils
import com.moji.hotfix.util.HotfixFileUtils
import com.moji.hotfix.util.HotfixMapUtils
import com.moji.hotfix.util.HotfixProcessor
import org.apache.commons.io.FileUtils
import org.gradle.api.Plugin
import org.gradle.api.Project

class HotfixPlugin implements Plugin<Project> {
  HashSet<String> includePackage
  HashSet<String> excludeClass
  HashSet<String> excludePackage = []
  def debugOn
  def beforeDexTasks = []

  private static final String MAPPING_TXT = "mapping.txt"
  private static final String HASH_TXT = "hash.txt"
  private static final String DEBUG = "debug"
  boolean isFix

  @Override
  void apply(Project project) {

    project.extensions.create("hotfix", HotfixExtension, project)

///**
// * 注册transform接口
// */
//    def isApp = project.plugins.hasPlugin(AppPlugin)
//    if (isApp) {
//      def android = project.extensions.getByType(AppExtension)
//      def transform = new TransformImpl(project)
//      android.registerTransform(transform)
//    }

    project.afterEvaluate {
      def extension = project.extensions.findByName("hotfix") as HotfixExtension
      includePackage = extension.includePackage
      excludeClass = extension.excludeClass
      debugOn = extension.debugOn
      isFix = extension.isFix

      extension.excludePackage.add("android.support.")
      extension.excludePackage.add("com.android.tools.")
      extension.excludePackage.add("com.moji.mjweather.hotfix.")
      extension.includePackage.add("com.moji.")

      extension.excludePackage.each {
        excludeName ->
          excludePackage.add(excludeName.replace(".", "/"))
      }

      project.android.applicationVariants.each { variant ->
        println("lastHashDir:Debug"+variant.name+":"+debugOn)
        if (!variant.name.contains(DEBUG) || (variant.name.contains(DEBUG) && debugOn)) {

          Map hashMap
          File nuwaDir
          File patchDir

//          def proguardTask = project.tasks.findByName("proguard${variant.name.capitalize()}")
          def proguardTask = project.tasks.findByName("transformClassesAndResourcesWithProguardFor${variant.name.capitalize()}")
          def processManifestTask = project.tasks.findByName(
              "process${variant.name.capitalize()}Manifest")
          def manifestFile = processManifestTask.outputs.files.files[0]

          println("manifestFile"+manifestFile+":"+variant.dirName)

          def dirName = variant.dirName
          nuwaDir = new File("${project.projectDir}/hotfix")
          def outputDir = new File("${nuwaDir}/${dirName}")
          if (isFix) {
            //def oldNuwaDir = HotfixFileUtils.getFileFromProperty(project, NUWA_DIR)
            def mappingFile = HotfixFileUtils.getVariantFile(nuwaDir, variant, MAPPING_TXT)
            HotfixAndroidUtils.applymapping(proguardTask, mappingFile)
            def hashFile = HotfixFileUtils.getVariantFile(nuwaDir, variant, HASH_TXT)
            hashMap = HotfixMapUtils.parseMap(hashFile)
            println("lastHashDir:Mapping"+mappingFile.absolutePath)
            println("lastHashDir:Hash"+hashFile.absolutePath)
          }



          def hashFile = new File(outputDir, "hash.txt")
          println("manifestFile"+hashFile.absolutePath)
          Closure nuwaPrepareClosure = {
            def applicationName = HotfixAndroidUtils.getApplication(manifestFile)
            if (applicationName != null) {
              excludeClass.add(applicationName)
            }

            outputDir.mkdirs()
            if (!hashFile.exists()) {
              hashFile.createNewFile()
            }else{
              hashFile.delete()
              hashFile.createNewFile()
            }

            if (isFix) {
              patchDir = new File("${nuwaDir}/${dirName}/patch")
              patchDir.mkdirs()
            }
            println("lastHashDir:CreateHash,Patch包"+patchDir+":")
          }

          Closure copyMappingClosure = {
            if (proguardTask) {
              def mapFile = new File(
                  "${project.buildDir}/outputs/mapping/${variant.dirName}/mapping.txt")
              def newMapFile = new File("${nuwaDir}/${variant.dirName}/mapping.txt");
              FileUtils.copyFile(mapFile, newMapFile)
              println("lastHashDir:copyMappingClosure")
            }
            if (patchDir) {
              HotfixAndroidUtils.dex(project, patchDir)
              HotfixAndroidUtils.signature(project,patchDir)
              println("lastHashDir:nuwaPatchDex")
            }
          }

          def dexTaskName = "transformClassesWithDexFor${variant.name.capitalize()}"
          def dexTask = project.tasks.findByName(dexTaskName)
          if (dexTask) {
            def hotfixJarBeforeDex = "hotfixJarBeforeDex${variant.name.capitalize()}"
            project.task(hotfixJarBeforeDex) << {
              Set<File> inputFiles = dexTask.inputs.files.files
              inputFiles.each { inputFile ->
                def path = inputFile.absolutePath
//                println("path:"+path)
                if (path.endsWith(".jar")) {
                  println("lastHashDir:ProcessJar")
                  HotfixProcessor.processJar(hashFile, inputFile, patchDir, hashMap, includePackage,excludePackage,
                      excludeClass)
                } else if (inputFile.isDirectory()) {
                  println("lastHashDir:ProcessClassPatch")
                  HotfixProcessor.processClassPath(hashFile,inputFile,patchDir,hashMap
                      ,includePackage,excludePackage
                      ,excludeClass)
                }
              }
            }
            def nuwaJarBeforeDexTask = project.tasks[hotfixJarBeforeDex]
            nuwaJarBeforeDexTask.dependsOn dexTask.taskDependencies.getDependencies(dexTask)
            dexTask.dependsOn nuwaJarBeforeDexTask

            nuwaJarBeforeDexTask.doFirst(nuwaPrepareClosure)
            nuwaJarBeforeDexTask.doLast(copyMappingClosure)

//            nuwaPatchTask.dependsOn nuwaJarBeforeDexTask
            beforeDexTasks.add(nuwaJarBeforeDexTask)
          } else {
            println("not found task:${dexTaskName}")
          }
        }
      }
    }
  }


}


