/*
 * IzPack - Copyright 2001-2008 Julien Ponge, All Rights Reserved.
 *
 * http://izpack.org/
 * http://izpack.codehaus.org/
 *
 * Copyright 2001 Johannes Lehtinen
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.izforge.izpack.data;


import com.izforge.izpack.api.data.binding.OsModel;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.Serializable;
import java.util.List;
import java.util.Map;

/**
 * Encloses information about a packed file. This class abstracts the way file data is stored to
 * package.
 *
 * @author Johannes Lehtinen <johannes.lehtinen@iki.fi>
 */
public class PackFile implements Serializable
{

    static final long serialVersionUID = -834377078706854909L;

    /**
     * Only available when compiling. Makes no sense when installing, use relativePath instead.
     */
    public transient String sourcePath = null;//should not be used anymore - may deprecate it.
    /**
     * The Path of the file relative to the given (compiletime's) basedirectory.
     * Can be resolved while installing with either current working directory or directory of "installer.jar".
     */
    protected String relativePath = null;

    /**
     * The full path name of the target file
     */
    private String targetPath = null;

    /**
     * The target operating system constraints of this file
     */
    private List<OsModel> osConstraints = null;

    /**
     * The length of the file in bytes
     */
    private long length = 0;

    /**
     * The size of the file used to calculate the pack size
     */
    private transient long size = 0;

    /**
     * The last-modification time of the file.
     */
    private long mtime = -1;

    /**
     * True if file is a directory (length should be 0 or ignored)
     */
    private boolean isDirectory = false;

    /**
     * Whether or not this file is going to override any existing ones
     */
    private OverrideType override;

    /**
     * Whether or not this file might be blocked by the operating system
     */
    private Blockable blockable = Blockable.BLOCKABLE_NONE;

    /**
     * Additional attributes or any else for customisation
     */
    private Map additionals = null;

    public String previousPackId = null;

    public long offsetInPreviousPack = -1;

    /**
     * True if the file is a Jar and pack200 compression us activated.
     */
    private boolean pack200Jar = false;

    /**
     * condition for this packfile
     */
    private String condition = null;

    /**
     * Constructs and initializes from a source file.
     *
     * @param baseDir  the baseDirectory of the Fileselection/compilation or null
     * @param src      file which this PackFile describes
     * @param target   the path to install the file to
     * @param osList   OS constraints
     * @param override what to do when the file already exists
     * @throws FileNotFoundException if the specified file does not exist.
     */
    public PackFile(File baseDir, File src, String target, List<OsModel> osList, OverrideType override, Blockable blockable)
            throws FileNotFoundException
    {
        this(src, computeRelativePathFrom(baseDir, src), target, osList, override, blockable, null);
    }

    /**
     * Constructs and initializes from a source file.
     *
     * @param src                file which this PackFile describes
     * @param relativeSourcePath the path relative to the compiletime's basedirectory, use computeRelativePathFrom(File, File) to compute this.
     * @param target             the path to install the file to
     * @param osList             OS constraints
     * @param override           what to do when the file already exists
     * @param additionals        additional attributes
     * @throws FileNotFoundException if the specified file does not exist.
     */
    public PackFile(File src, String relativeSourcePath, String target, List<OsModel> osList, OverrideType override, Blockable blockable, Map additionals)
            throws FileNotFoundException
    {
        if (!src.exists()) // allows cleaner client co
        {
            throw new FileNotFoundException("No such file: " + src);
        }

        if ('/' != File.separatorChar)
        {
            target = target.replace(File.separatorChar, '/');
        }
        if (target.endsWith("/"))
        {
            target = target.substring(0, target.length() - 1);
        }

        this.sourcePath = src.getPath().replace(File.separatorChar, '/');
        this.relativePath = (relativeSourcePath != null) ? relativeSourcePath.replace(File.separatorChar, '/') : relativeSourcePath;

        this.targetPath = (target != null) ? target.replace(File.separatorChar, '/') : target;
        this.osConstraints = osList;
        this.override = override;
        this.blockable = blockable;

        this.length = src.length();
        this.size = this.length;
        this.mtime = src.lastModified();
        this.isDirectory = src.isDirectory();
        this.additionals = additionals;

        // File.length is undefined for directories - we don't add any data, so don't skip
        // any please!
        if (isDirectory)
        {
            length = 0;
        }
    }

    /**
     * Constructs and initializes from a source file.
     *
     * @param baseDir     The Base directory that is used to search for the files. This is used to build the relative path's
     * @param src         file which this PackFile describes
     * @param target      the path to install the file to
     * @param osList      OS constraints
     * @param override    what to do when the file already exists
     * @param additionals additional attributes
     * @throws FileNotFoundException if the specified file does not exist.
     */
    public PackFile(File baseDir, File src, String target, List<OsModel> osList, OverrideType override, Blockable blockable, Map additionals)
            throws FileNotFoundException
    {
        this(src, computeRelativePathFrom(baseDir, src), target, osList, override, blockable, additionals);
    }

    /**
     * Builds the relative path of file to the baseDir.
     *
     * @param baseDir The Base Directory to build the relative path from
     * @param file    the file inside basDir
     * @return null if file is not a inside baseDir
     */
    public static String computeRelativePathFrom(File baseDir, File file)
    {
        if (baseDir == null || file == null)
        {
            return null;
        }
        try
        { // extract relative path...
            if (file.getAbsolutePath().startsWith(baseDir.getAbsolutePath()))
            {
                return file.getAbsolutePath().substring(baseDir.getAbsolutePath().length() + 1);
            }
        }
        catch (Exception x)// don't throw an exception here. return null instead!
        {
            // if we cannot build the relative path because of an error, the developer should be
            // informed about.
            x.printStackTrace();
        }

        // we can not build a relative path for whatever reason
        return null;
    }

    public void setPreviousPackFileRef(String previousPackId, Long offsetInPreviousPack)
    {
        this.previousPackId = previousPackId;
        this.offsetInPreviousPack = offsetInPreviousPack;
    }

    /**
     * The target operating system constraints of this file
     */
    public final List<OsModel> osConstraints()
    {
        return osConstraints;
    }

    /**
     * The length of the file in bytes
     */
    public final long length()
    {
        return length;
    }

    /**
     * The size of the file in bytes (is the same as the length if it is not a loose pack)
     */
    public final long size()
    {
        return size;
    }

    /**
     * The last-modification time of the file.
     */
    public final long lastModified()
    {
        return mtime;
    }

    /**
     * Whether or not this file is going to override any existing ones
     */
    public final OverrideType override()
    {
        return override;
    }

    /**
     * Whether or not this file might be blocked during installation/uninstallation
     */
    public final Blockable blockable()
    {
        return blockable;
    }

    public final boolean isDirectory()
    {
        return isDirectory;
    }

    public final boolean isBackReference()
    {
        return (previousPackId != null);
    }

    /**
     * The full path name of the target file, using '/' as fileseparator.
     */
    public final String getTargetPath()
    {
        return targetPath;
    }

    /**
     * The Path of the file relative to the given (compiletime's) basedirectory.
     * Can be resolved while installing with either current working directory or directory of "installer.jar"
     */
    public String getRelativeSourcePath()
    {
        return relativePath;
    }

    /**
     * Returns the additionals map.
     *
     * @return additionals
     */
    public Map getAdditionals()
    {
        return additionals;
    }


    /**
     * @return the condition
     */
    public String getCondition()
    {
        return this.condition;
    }


    /**
     * @param condition the condition to set
     */
    public void setCondition(String condition)
    {
        this.condition = condition;
    }

    public boolean hasCondition()
    {
        return this.condition != null;
    }

    public boolean isPack200Jar()
    {
        return pack200Jar;
    }

    public void setPack200Jar(boolean pack200Jar)
    {
        this.pack200Jar = pack200Jar;
    }

    public void setLoosePackInfo(boolean loose)
    {
        if (loose)
        {
            // file is part of a loose pack
            length = 0;
        }
    }
}
