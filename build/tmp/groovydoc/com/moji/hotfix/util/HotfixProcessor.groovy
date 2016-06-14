package com.moji.hotfix.util

import org.apache.commons.codec.digest.DigestUtils
import org.apache.commons.io.IOUtils
import org.objectweb.asm.*

import java.util.jar.JarEntry
import java.util.jar.JarFile
import java.util.jar.JarOutputStream
import java.util.zip.ZipEntry

/**
 * Created by yilong.zhang on 16/5/21.
 */
class HotfixProcessor {

  public
  static void processClassPath( File hashFile, File classPath, File patchDir, Map map
      , HashSet<String> includePackage, HashSet<String> excludePackage,HashSet<String> excludeClass) {
    println("trav file inputFile:begin"+File.separator)
    File[] classfiles = classPath.listFiles()

    classfiles.each { inputFile ->
      def path = inputFile.absolutePath
      //path = path.split("${dirName}/")[1]
      if (inputFile.isDirectory()) {
        println("trav file isDirectory")
        processClassPath(hashFile, inputFile, patchDir, map, includePackage,excludePackage,excludeClass)
      } else if (path.endsWith(".jar")) {
        println("trav file jar:"+path)
        processJar(hashFile, inputFile, patchDir, map, includePackage, excludePackage,excludeClass)
//        return;
      } else if (shouldProcessClass(path,includePackage,excludePackage,excludeClass)) {
            def bytes = processClass(inputFile)
            def hash = DigestUtils.shaHex(bytes)
            hashFile.append(HotfixMapUtils.format(path, hash))
            if (HotfixMapUtils.notSame(map, path, hash)) {
              println("patch class:" + path)
              HotfixFileUtils.copyBytesToFile(inputFile.bytes, HotfixFileUtils.touchFile(patchDir, path))
            }
//        return;
        println("trav file Class:"+path)
      }

    }
  }


  public
  static processJar(File hashFile, File jarFile, File patchDir, Map map,
      HashSet<String> includePackage, HashSet<String> excludePackage,HashSet<String> excludeClass) {
    if (jarFile) {
      def optJar = new File(jarFile.getParent(), jarFile.name + ".opt")

      def file = new JarFile(jarFile);
      Enumeration enumeration = file.entries();
      JarOutputStream jarOutputStream = new JarOutputStream(new FileOutputStream(optJar));

      while (enumeration.hasMoreElements()) {
        JarEntry jarEntry = (JarEntry) enumeration.nextElement();
        String entryName = jarEntry.getName();
        ZipEntry zipEntry = new ZipEntry(entryName);

        InputStream inputStream = file.getInputStream(jarEntry);
        jarOutputStream.putNextEntry(zipEntry);

        if (shouldProcessClassInJar(entryName, includePackage, excludePackage, excludeClass)) {
          def bytes = referHackWhenInit(inputStream);
          jarOutputStream.write(bytes);

          def hash = DigestUtils.shaHex(bytes)
          hashFile.append(HotfixMapUtils.format(entryName, hash))

          if (HotfixMapUtils.notSame(map, entryName, hash)) {
            HotfixFileUtils.copyBytesToFile(bytes, HotfixFileUtils.touchFile(patchDir, entryName))
          }
        } else {
          jarOutputStream.write(IOUtils.toByteArray(inputStream));
        }
        jarOutputStream.closeEntry();
      }
      jarOutputStream.close();
      file.close();

      if (jarFile.exists()) {
        jarFile.delete()
      }
      optJar.renameTo(jarFile)
    }
  }

  //refer hack class when object init
  private static byte[] referHackWhenInit(InputStream inputStream) {
    ClassReader cr = new ClassReader(inputStream);
    ClassWriter cw = new ClassWriter(cr, 0);
    ClassVisitor cv = new ClassVisitor(Opcodes.ASM4, cw) {
      @Override
      public MethodVisitor visitMethod(int access, String name, String desc,
          String signature, String[] exceptions) {

        MethodVisitor mv = super.visitMethod(access, name, desc, signature, exceptions);
        mv = new MethodVisitor(Opcodes.ASM4, mv) {
          @Override
          void visitInsn(int opcode) {
            if ("<init>".equals(name) && opcode == Opcodes.RETURN) {
              super.visitLdcInsn(Type.getType("Lcom/moji/mjweather/hotfix/Hack;"));
            }
            super.visitInsn(opcode);
          }
        }
        return mv;
      }
    };
    cr.accept(cv, 0);
    return cw.toByteArray();
  }

  public static boolean shouldProcessClass(String path, HashSet<String> includePackage
                                           ,HashSet<String> excludePackage,
                                           HashSet<String> excludeClass) {
    boolean isClassOk = path.endsWith(".class") && (!path.contains(File.separator+"R\$") && !path.endsWith(File.separator+"R.class")
            && !path.endsWith(File.separator+"BuildConfig.class"))
    boolean isPackageOk = HotfixSetUtils.isIncluded(path, includePackage)&&!HotfixSetUtils.isExcluded(path, excludePackage,excludeClass,includePackage)
    return isClassOk&&isPackageOk;
  }

  private
  static boolean shouldProcessClassInJar(String entryName, HashSet<String> includePackage
      ,HashSet<String> excludePackage,
      HashSet<String> excludeClass) {
    return entryName.endsWith(".class") &&
        HotfixSetUtils.isIncluded(entryName, includePackage) &&
        !HotfixSetUtils.isExcluded(entryName, excludePackage,excludeClass,includePackage)
  }

  public static byte[] processClass(File file) {
    def optClass = new File(file.getParent(), file.name + ".opt")

    FileInputStream inputStream = new FileInputStream(file);
    FileOutputStream outputStream = new FileOutputStream(optClass)

    def bytes = referHackWhenInit(inputStream);
    outputStream.write(bytes)
    inputStream.close()
    outputStream.close()
    if (file.exists()) {
      file.delete()
    }
    optClass.renameTo(file)
    return bytes
  }
}
