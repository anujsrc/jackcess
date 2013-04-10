/*
Copyright (c) 2011 James Ahlborn

This library is free software; you can redistribute it and/or
modify it under the terms of the GNU Lesser General Public
License as published by the Free Software Foundation; either
version 2.1 of the License, or (at your option) any later version.

This library is distributed in the hope that it will be useful,
but WITHOUT ANY WARRANTY; without even the implied warranty of
MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
Lesser General Public License for more details.

You should have received a copy of the GNU Lesser General Public
License along with this library; if not, write to the Free Software
Foundation, Inc., 59 Temple Place, Suite 330, Boston, MA  02111-1307
USA
*/

package com.healthmarketscience.jackcess.impl.complex;

import java.io.IOException;
import java.util.Date;

import com.healthmarketscience.jackcess.Column;
import com.healthmarketscience.jackcess.Row;
import com.healthmarketscience.jackcess.Table;
import com.healthmarketscience.jackcess.complex.Attachment;
import com.healthmarketscience.jackcess.complex.AttachmentColumnInfo;
import com.healthmarketscience.jackcess.complex.ComplexDataType;
import com.healthmarketscience.jackcess.complex.ComplexValue;
import com.healthmarketscience.jackcess.complex.ComplexValueForeignKey;
import com.healthmarketscience.jackcess.impl.ByteUtil;


/**
 * Complex column info for a column holding 0 or more attachments per row.
 *
 * @author James Ahlborn
 */
public class AttachmentColumnInfoImpl extends ComplexColumnInfoImpl<Attachment>
  implements AttachmentColumnInfo
{
  private static final String FILE_NAME_COL_NAME = "FileName";
  private static final String FILE_TYPE_COL_NAME = "FileType";

  private final Column _fileUrlCol;
  private final Column _fileNameCol;
  private final Column _fileTypeCol;
  private final Column _fileDataCol;
  private final Column _fileTimeStampCol;
  private final Column _fileFlagsCol;
  
  public AttachmentColumnInfoImpl(Column column, int complexId,
                                  Table typeObjTable, Table flatTable)
    throws IOException
  {
    super(column, complexId, typeObjTable, flatTable);

    Column fileUrlCol = null;
    Column fileNameCol = null;
    Column fileTypeCol = null;
    Column fileDataCol = null;
    Column fileTimeStampCol = null;
    Column fileFlagsCol = null;

    for(Column col : getTypeColumns()) {
      switch(col.getType()) {
      case TEXT:
        if(FILE_NAME_COL_NAME.equalsIgnoreCase(col.getName())) {
          fileNameCol = col;
        } else if(FILE_TYPE_COL_NAME.equalsIgnoreCase(col.getName())) {
          fileTypeCol = col;
        } else {
          // if names don't match, assign in order: name, type
          if(fileNameCol == null) {
            fileNameCol = col;
          } else if(fileTypeCol == null) {
            fileTypeCol = col;
          }
        }
        break;
      case LONG:
        fileFlagsCol = col;
        break;
      case SHORT_DATE_TIME:
        fileTimeStampCol = col;
        break;
      case OLE:
        fileDataCol = col;
        break;
      case MEMO:
        fileUrlCol = col;
        break;
      default:
        // ignore
      }
    }
    
    _fileUrlCol = fileUrlCol;
    _fileNameCol = fileNameCol;
    _fileTypeCol = fileTypeCol;
    _fileDataCol = fileDataCol;
    _fileTimeStampCol = fileTimeStampCol;
    _fileFlagsCol = fileFlagsCol;
  }

  public Column getFileUrlColumn() {
    return _fileUrlCol;
  }
  
  public Column getFileNameColumn() {
    return _fileNameCol;
  }

  public Column getFileTypeColumn() {
    return _fileTypeCol;
  }
  
  public Column getFileDataColumn() {
    return _fileDataCol;
  }
  
  public Column getFileTimeStampColumn() {
    return _fileTimeStampCol;
  }
  
  public Column getFileFlagsColumn() {
    return _fileFlagsCol;
  }  
  
  @Override
  public ComplexDataType getType()
  {
    return ComplexDataType.ATTACHMENT;
  }

  @Override
  protected AttachmentImpl toValue(ComplexValueForeignKey complexValueFk,
                                   Row rawValue) {
    ComplexValue.Id id = getValueId(rawValue);
    String url = (String)getFileUrlColumn().getRowValue(rawValue);
    String name = (String)getFileNameColumn().getRowValue(rawValue);
    String type = (String)getFileTypeColumn().getRowValue(rawValue);
    Integer flags = (Integer)getFileFlagsColumn().getRowValue(rawValue);
    Date ts = (Date)getFileTimeStampColumn().getRowValue(rawValue);
    byte[] data = (byte[])getFileDataColumn().getRowValue(rawValue);
    
    return new AttachmentImpl(id, complexValueFk, url, name, type, data,
                              ts, flags);
  }

  @Override
  protected Object[] asRow(Object[] row, Attachment attachment) {
    super.asRow(row, attachment);
    getFileUrlColumn().setRowValue(row, attachment.getFileUrl());
    getFileNameColumn().setRowValue(row, attachment.getFileName());
    getFileTypeColumn().setRowValue(row, attachment.getFileType());
    getFileFlagsColumn().setRowValue(row, attachment.getFileFlags());
    getFileTimeStampColumn().setRowValue(row, attachment.getFileTimeStamp());
    getFileDataColumn().setRowValue(row, attachment.getFileData());
    return row;
  }

  public static Attachment newAttachment(byte[] data) {
    return newAttachment(INVALID_FK, data);
  }
  
  public static Attachment newAttachment(ComplexValueForeignKey complexValueFk,
                                         byte[] data) {
    return newAttachment(complexValueFk, null, null, null, data, null, null);
  }

  public static Attachment newAttachment(
      String url, String name, String type, byte[] data,
      Date timeStamp, Integer flags)
  {
    return newAttachment(INVALID_FK, url, name, type, data,
                         timeStamp, flags);
  }
  
  public static Attachment newAttachment(
      ComplexValueForeignKey complexValueFk, String url, String name,
      String type, byte[] data, Date timeStamp, Integer flags)
  {
    return new AttachmentImpl(INVALID_ID, complexValueFk, url, name, type,
                              data, timeStamp, flags);
  }

  
  private static class AttachmentImpl extends ComplexValueImpl
    implements Attachment
  {
    private String _url;
    private String _name;
    private String _type;
    private byte[] _data;
    private Date _timeStamp;
    private Integer _flags;

    private AttachmentImpl(Id id, ComplexValueForeignKey complexValueFk,
                           String url, String name, String type, byte[] data,
                           Date timeStamp, Integer flags)
    {
      super(id, complexValueFk);
      _url = url;
      _name = name;
      _type = type;
      _data = data;
      _timeStamp = timeStamp;
      _flags = flags;
    }
    
    public byte[] getFileData() {
      return _data;
    }

    public void setFileData(byte[] data) {
      _data = data;
    }

    public String getFileName() {
      return _name;
    }

    public void setFileName(String fileName) {
      _name = fileName;
    }
  
    public String getFileUrl() {
      return _url;
    }

    public void setFileUrl(String fileUrl) {
      _url = fileUrl;
    }
  
    public String getFileType() {
      return _type;
    }

    public void setFileType(String fileType) {
      _type = fileType;
    }
  
    public Date getFileTimeStamp() {
      return _timeStamp;
    }

    public void setFileTimeStamp(Date fileTimeStamp) {
      _timeStamp = fileTimeStamp;
    }
  
    public Integer getFileFlags() {
      return _flags;
    }

    public void setFileFlags(Integer fileFlags) {
      _flags = fileFlags;
    }  

    public void update() throws IOException {
      getComplexValueForeignKey().updateAttachment(this);
    }
    
    public void delete() throws IOException {
      getComplexValueForeignKey().deleteAttachment(this);
    }
    
    @Override
    public String toString()
    {
      return "Attachment(" + getComplexValueForeignKey() + "," + getId() +
        ") " + getFileUrl() + ", " + getFileName() + ", " + getFileType()
        + ", " + getFileTimeStamp() + ", " + getFileFlags()  + ", " +
        ByteUtil.toHexString(getFileData());
    } 
  }
  
}