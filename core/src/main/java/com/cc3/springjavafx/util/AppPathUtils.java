package com.cc3.springjavafx.util;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.IOException;
import java.net.URI;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Comparator;
import java.util.stream.Stream;

/**
 * 路径工具
 */
public class AppPathUtils {
    
    private static final File JAR_DIR;
    private static final String JAR_DIR_PATH;
    private static final Logger logger = LoggerFactory.getLogger(AppPathUtils.class);

    static {
        JAR_DIR = initJarDirectory();
        JAR_DIR_PATH = JAR_DIR.getAbsolutePath();
    }
    
    private static File initJarDirectory() {
        try {
            URL url = AppPathUtils.class.getProtectionDomain()
                    .getCodeSource()
                    .getLocation();
            if (url != null) {
                return resolveAppDir(url).toFile();
            }
        } catch (Throwable e) {
            // 降级：使用当前工作目录
            logger.error("获取 JAR 所在目录失败！降级使用当前工作目录", e);
        }
        // 降级：使用当前工作目录
        return new File(System.getProperty("user.dir"));
    }

    private static Path resolveAppDir(URL url) throws Exception {
        String protocol = url.getProtocol();

        if ("jar".equals(protocol)) {
            // Spring Boot: jar:file:/C:/中文路径/mail-tool.jar!/BOOT-INF/classes!/
            String s = url.toString().substring(4); // 去掉 "jar:"
            int idx = s.indexOf("!/");
            if (idx > 0) {
                s = s.substring(0, idx);
            }
            // 关键：用 URI 中转，避免 URL.getPath() 的中文乱码
            File jarFile = new File(new URI(s));
            return jarFile.getParentFile().toPath().toAbsolutePath().normalize();

        } else if ("file".equals(protocol)) {
            // 开发环境 class 目录，或普通 jar
            File file = new File(url.toURI());
            if (file.isFile()) {
                return file.getParentFile().toPath().toAbsolutePath().normalize();
            } else {
                // target/classes 或 bin，向上退到项目根目录
                Path p = file.toPath().toAbsolutePath().normalize();
                if (p.endsWith("classes") || p.endsWith("bin")) {
                    Path target = p.getParent();
                    if (target != null) {
                        Path root = target.getParent();
                        if (root != null) return root;
                    }
                }
                return p;
            }
        }
        return new File(url.toURI()).toPath().toAbsolutePath().normalize();
    }
    
    /** 获取 JAR 所在目录 */
    public static File getAppDir() {
        return JAR_DIR;
    }

    /** 获取 JAR 所在目录的路径字符串 */
    public static String getAppDirPath() {
        return JAR_DIR_PATH;
    }
    
    /** 获取 JAR 同级目录下的子文件夹 */
    public static File getSubDir(String name) {
        File dir = new File(JAR_DIR, name);
        if (!dir.exists()) {
            dir.mkdirs();
        }
        return dir;
    }
    
    /** 获取 JAR 同级目录下的文件 */
    public static File getFile(String... paths) {
        Path path = JAR_DIR.toPath();
        for (String p : paths) {
            path = path.resolve(p);
        }
        return path.toFile();
    }

    /** 获取 JAR 同级目录下的文件 */
    public static File getFile(String path) {
        return getFile(path.split("/"));
    }
    
    /** 获取 JAR 同级目录下的路径字符串 */
    public static String getPath(String... paths) {
        return getFile(paths).getAbsolutePath();
    }

    /** 获取 JAR 同级目录下的路径字符串 */
    public static String getPath(String path) {
        return getPath(path.split("/"));
    }

    /**
     * 创建文件夹
     * @param path 路径
     * @return 是否创建成功
     */
    public static boolean createDir(String path) {
        if (path == null || path.trim().isEmpty()) {
            logger.error("创建文件夹失败：路径为空");
            return false;
        }
        Path optPath = Paths.get(path);
        if (!Files.exists(optPath)) {
            try {
                Files.createDirectories(optPath);
            } catch (IOException e) {
                logger.error("创建文件夹时发生IO错误！", e);
                return false;
            }
        }
        return Files.isDirectory(optPath);
    }

    /**
     * 删除文件夹
     * @param dirPath 文件夹路径
     * @return 是否删除成功（有任何一个文件删不掉就返回 false，不再"假成功"）
     */
    public static boolean deleteFiles(String dirPath) {
        if (dirPath == null || dirPath.trim().isEmpty()) {
            logger.error("删除文件夹失败：路径为空");
            return false;
        }
        return deleteFiles(Paths.get(dirPath));
    }
    /**
     * 删除文件夹
     * @param dirPath 文件夹路径
     * @return 是否删除成功（有任何一个文件删不掉就返回 false）
     */
    public static boolean deleteFiles(Path dirPath) {
        if (dirPath == null || !Files.exists(dirPath)) {
            return true;
        }
        final boolean[] success = {true};
        try (Stream<Path> walk = Files.walk(dirPath)) {
            walk.sorted(Comparator.reverseOrder()) // 从深层到浅层排序，确保先删子项
                    .forEach(p -> {
                        try {
                            Files.delete(p);
                        } catch (IOException e) {
                            success[0] = false;
                            logger.error("删除失败，文件路径：{}；原因：{}", p, e.getMessage());
                        }
                    });
        } catch (IOException e) {
            logger.error("IOException 删除失败！", e);
            return false;
        }
        return success[0];
    }
}