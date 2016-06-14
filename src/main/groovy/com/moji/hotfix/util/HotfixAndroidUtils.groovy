package com.moji.hotfix.util

import org.apache.tools.ant.taskdefs.condition.Os
import org.apache.tools.ant.util.JavaEnvUtils
import org.gradle.api.DefaultTask
import org.gradle.api.GradleException
import org.gradle.api.InvalidUserDataException
import org.gradle.api.Project

/**
 * Created by yilong.zhang on 16/5/21.
 */
class HotfixAndroidUtils {

    private static final String PATCH_NAME = "patch.jar"

    public static String getApplication(File manifestFile) {
        try {
            def manifest = new XmlParser().parse(manifestFile)
            def androidTag = new groovy.xml.Namespace("http://schemas.android.com/apk/res/android", 'android')
            def applicationName = manifest.application[0].attribute(androidTag.name)

            if (applicationName != null) {
                return applicationName.replace(".", "/") + ".class"
            }
        }catch (Exception e) {
            e.printStackTrace()
        }

        return "com/moji/mjweather/MJApplication.class";
    }

    public static dex(Project project, File classDir) {
        if (classDir.listFiles().size()) {
            def sdkDir
  
            Properties properties = new Properties()
            File localProps = project.rootProject.file("local.properties")
            if (localProps.exists()) {
                properties.load(localProps.newDataInputStream())
                sdkDir = properties.getProperty("sdk.dir")
            } else {
                sdkDir = System.getenv("ANDROID_HOME")
            }
            if (sdkDir) {
                def cmdExt = Os.isFamily(Os.FAMILY_WINDOWS) ? '.bat' : ''
                def stdout = new ByteArrayOutputStream()
                project.exec {
                    commandLine "${sdkDir}/build-tools/${project.android.buildToolsVersion}/dx${cmdExt}",
                            '--dex',
                            "--output=${new File(classDir.getParent(), PATCH_NAME).absolutePath}",
                            "${classDir.absolutePath}"
                    standardOutput = stdout
                }
                def error = stdout.toString().trim()
                if (error) {
                    println "dex error:" + error
                }
            } else {
                throw new InvalidUserDataException('$ANDROID_HOME is not defined')
            }
        }
    }

    public static applymapping(DefaultTask proguardTask, File mappingFile) {
        if (proguardTask) {
            if (mappingFile.exists()) {
//                proguardTask.applymapping(mappingFile)
            } else {
                println "$mappingFile does not exist"
            }
        }
    }

    /**
     * 签名补丁
     */
    static void signPatch(Project project,File patchFile) {

        def File keystore = project.file("mojiweather2.keystore")
        def String keyPassword = "mojiandroid"
        def String keyalis = "moji"
        if(!patchFile.exists() || !keystore.exists()) {
            return
        }

        def args = [JavaEnvUtils.getJdkExecutable('jarsigner'),
                    '-verbose',
                    '-sigalg', 'MD5withRSA',
                    '-digestalg', 'SHA1',
                    '-keystore', keystore.absolutePath,
                    '-keypass', keyPassword,
                    '-storepass', keyPassword,
                    new File(patchFile.getParent(),PATCH_NAME),
                    keyalis]

        println "Signing with command:"
        for (String s : args)
            print s + " "
        println ""
        def proc = args.execute()
        def outRedir = new StreamRedir(proc.inputStream, System.out)
        def errRedir = new StreamRedir(proc.errorStream, System.out)

        outRedir.start()
        errRedir.start()

        def result = proc.waitFor()
        outRedir.join()
        errRedir.join()

        if (result != 0) {
            throw new GradleException('Couldn\'t sign')
        }
    }

    static class StreamRedir extends Thread {
        private inStream
        private outStream

        public StreamRedir(inStream, outStream) {
            this.inStream = inStream
            this.outStream = outStream
        }

        public void run() {
            int b;
            while ((b = inStream.read()) != -1)
                outStream.write(b)
        }
    }
}
