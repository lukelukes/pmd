/**
 * BSD-style license; for more info see http://pmd.sourceforge.net/license.html
 */

package net.sourceforge.pmd.util;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FilenameFilter;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Enumeration;
import java.util.List;
import java.util.Locale;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;

import org.apache.commons.io.FileUtils;
import org.apache.commons.lang3.StringUtils;

import net.sourceforge.pmd.annotation.InternalApi;
import net.sourceforge.pmd.util.datasource.DataSource;
import net.sourceforge.pmd.util.datasource.FileDataSource;
import net.sourceforge.pmd.util.datasource.ZipDataSource;
import net.sourceforge.pmd.util.filter.AndFilter;
import net.sourceforge.pmd.util.filter.Filter;
import net.sourceforge.pmd.util.filter.Filters;
import net.sourceforge.pmd.util.filter.OrFilter;

/**
 * This is a utility class for working with Files.
 * @deprecated Is internal API
 */
@Deprecated
@InternalApi
public final class FileUtil {

    private FileUtil() {
    }

    /**
     * Helper method to get a filename without its extension
     *
     * @param fileName
     *            String
     * @return String
     */
    public static String getFileNameWithoutExtension(String fileName) {
        String name = fileName;

        int index = fileName.lastIndexOf('.');
        if (index != -1) {
            name = fileName.substring(0, index);
        }

        return name;
    }

    /**
     * Normalizes the filename by taking the casing into account, e.g. on
     * Windows, the filename is changed to lowercase only.
     *
     * @param fileName
     *            the file name
     * @return the normalized file name
     */
    public static String normalizeFilename(String fileName) {
        if (fileName != null && File.separatorChar == '\\') {
            // windows
            return fileName.toLowerCase(Locale.ROOT);
        }
        return fileName;
    }

    /**
     * Collects a list of DataSources using a comma separated list of input file
     * locations to process. If a file location is a directory, the directory
     * hierarchy will be traversed to look for files. If a file location is a
     * ZIP or Jar the archive will be scanned looking for files. If a file
     * location is a file, it will be used. For each located file, a
     * FilenameFilter is used to decide whether to return a DataSource.
     *
     * @param fileLocations
     *            A comma-separated list of file locations.
     * @param filenameFilter
     *            The FilenameFilter to apply to files.
     * @return A list of DataSources, one for each file collected.
     */
    public static List<DataSource> collectFiles(String fileLocations, FilenameFilter filenameFilter) {
        List<DataSource> dataSources = new ArrayList<>();
        for (String fileLocation : fileLocations.split(",")) {
            collect(dataSources, fileLocation, filenameFilter);
        }
        return dataSources;
    }

    private static List<DataSource> collect(List<DataSource> dataSources, String fileLocation,
                                            FilenameFilter filenameFilter) {
        File file = new File(fileLocation);
        if (!file.exists()) {
            throw new RuntimeException("File " + file.getName() + " doesn't exist");
        }
        if (!file.isDirectory()) {
            if (fileLocation.endsWith(".zip") || fileLocation.endsWith(".jar")) {
                @SuppressWarnings("PMD.CloseResource")
                // the zip file can't be closed here, it needs to be closed at the end of the PMD run
                // see net.sourceforge.pmd.processor.AbstractPMDProcessor#processFiles(...)
                ZipFile zipFile;
                try {
                    zipFile = new ZipFile(fileLocation);
                    Enumeration<? extends ZipEntry> e = zipFile.entries();
                    while (e.hasMoreElements()) {
                        ZipEntry zipEntry = e.nextElement();
                        if (filenameFilter.accept(null, zipEntry.getName())) {
                            dataSources.add(new ZipDataSource(zipFile, zipEntry));
                        }
                    }
                } catch (IOException ze) {
                    throw new RuntimeException("Archive file " + file.getName() + " can't be opened");
                }
            } else {
                dataSources.add(new FileDataSource(file));
            }
        } else {
            // Match files, or directories which are not excluded.
            // FUTURE Make the excluded directories be some configurable option
            Filter<File> filter = new OrFilter<>(Filters.toFileFilter(filenameFilter),
                    new AndFilter<>(Filters.getDirectoryFilter(), Filters.toNormalizedFileFilter(
                            Filters.buildRegexFilterExcludeOverInclude(null, Collections.singletonList("SCCS")))));
            FileFinder finder = new FileFinder();
            List<File> files = finder.findFilesFrom(file, Filters.toFilenameFilter(filter), true);
            for (File f : files) {
                dataSources.add(new FileDataSource(f));
            }
        }
        return dataSources;
    }

    /**
     * Handy method to find a certain pattern into a file. While this method
     * lives in the FileUtils, it was designed with with unit test in mind (to
     * check result redirected into a file)
     *
     * @param file
     * @param pattern
     * @return
     */
    public static boolean findPatternInFile(final File file, final String pattern) {

        Pattern regexp = Pattern.compile(pattern);
        Matcher matcher = regexp.matcher("");

        try {
            for (String line : Files.readAllLines(file.toPath(), StandardCharsets.UTF_8)) {
                matcher.reset(line); // reset the input
                if (matcher.find()) {
                    return true;
                }
            }
        } catch (FileNotFoundException e) {
            throw new IllegalArgumentException(e);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }

        return false;
    }

    /**
     * Reads the file, which contains the filelist. This is used for the
     * command line arguments --filelist/-filelist for both PMD and CPD.
     * The separator in the filelist is a comma and/or newlines.
     *
     * @param filelist the file which contains the list of path names
     * @return a comma-separated list of file paths
     * @throws IOException if the file couldn't be read
     */
    public static String readFilelist(File filelist) throws IOException {
        String filePaths = FileUtils.readFileToString(filelist);
        filePaths = StringUtils.trimToEmpty(filePaths);
        filePaths = filePaths.replaceAll("\\r?\\n", ",");
        filePaths = filePaths.replaceAll(",+", ",");
        return filePaths;
    }
}
