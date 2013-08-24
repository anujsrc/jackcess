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

package com.healthmarketscience.jackcess;

import java.io.File;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;

import junit.framework.TestCase;
import com.healthmarketscience.jackcess.impl.PropertyMapImpl;
import com.healthmarketscience.jackcess.impl.PropertyMaps;
import static com.healthmarketscience.jackcess.Database.*;
import static com.healthmarketscience.jackcess.DatabaseTest.*;
import static com.healthmarketscience.jackcess.impl.JetFormatTest.*;
import com.healthmarketscience.jackcess.impl.TableImpl;
import com.healthmarketscience.jackcess.impl.DatabaseImpl;

/**
 * @author James Ahlborn
 */
public class PropertiesTest extends TestCase
{

  public PropertiesTest(String name) throws Exception {
    super(name);
  }

  public void testPropertyMaps() throws Exception
  {
    PropertyMaps maps = new PropertyMaps(10);
    assertTrue(maps.isEmpty());
    assertEquals(0, maps.getSize());
    assertFalse(maps.iterator().hasNext());
    assertEquals(10, maps.getObjectId());

    PropertyMapImpl defMap = maps.getDefault();
    assertTrue(defMap.isEmpty());
    assertEquals(0, defMap.getSize());
    assertFalse(defMap.iterator().hasNext());

    PropertyMapImpl colMap = maps.get("testcol");
    assertTrue(colMap.isEmpty());
    assertEquals(0, colMap.getSize());
    assertFalse(colMap.iterator().hasNext());
    
    assertFalse(maps.isEmpty());
    assertEquals(2, maps.getSize());
    
    assertSame(defMap, maps.get(PropertyMaps.DEFAULT_NAME));
    assertEquals(PropertyMaps.DEFAULT_NAME, defMap.getName());
    assertSame(colMap, maps.get("TESTCOL"));
    assertEquals("testcol", colMap.getName());

    defMap.put("foo", DataType.TEXT, (byte)0, "bar");
    defMap.put("baz", DataType.LONG, (byte)1, 13);

    assertFalse(defMap.isEmpty());
    assertEquals(2, defMap.getSize());

    colMap.put("buzz", DataType.BOOLEAN, (byte)0, Boolean.TRUE);

    assertFalse(colMap.isEmpty());
    assertEquals(1, colMap.getSize());

    assertEquals("bar", defMap.getValue("foo"));
    assertEquals("bar", defMap.getValue("FOO"));
    assertNull(colMap.getValue("foo"));
    assertEquals(13, defMap.get("baz").getValue());
    assertEquals(Boolean.TRUE, colMap.getValue("Buzz"));

    assertEquals("bar", defMap.getValue("foo", "blah"));
    assertEquals("blah", defMap.getValue("bogus", "blah"));

    List<PropertyMap.Property> props = new ArrayList<PropertyMap.Property>();
    for(PropertyMap map : maps) {
      for(PropertyMap.Property prop : map) {
        props.add(prop);
      }
    }

    assertEquals(Arrays.asList(defMap.get("foo"), defMap.get("baz"), 
                               colMap.get("buzz")), props);
  }

  public void testReadProperties() throws Exception
  {
    byte[] nameMapBytes = null;

    for(TestDB testDb : SUPPORTED_DBS_TEST_FOR_READ) {
      Database db = open(testDb);

      TableImpl t = (TableImpl)db.getTable("Table1");
      assertEquals(t.getTableDefPageNumber(), 
                   t.getPropertyMaps().getObjectId());
      PropertyMap tProps = t.getProperties();
      assertEquals(PropertyMaps.DEFAULT_NAME, tProps.getName());
      int expectedNumProps = 3;
      if(db.getFileFormat() != Database.FileFormat.V1997) {
        assertEquals("{5A29A676-1145-4D1A-AE47-9F5415CDF2F1}", 
                     tProps.getValue(PropertyMap.GUID_PROP));
        if(nameMapBytes == null) {
          nameMapBytes = (byte[])tProps.getValue("NameMap");
        } else {
          assertTrue(Arrays.equals(nameMapBytes, 
                                   (byte[])tProps.getValue("NameMap")));
        }
        expectedNumProps += 2;
      }  
      assertEquals(expectedNumProps, tProps.getSize());
      assertEquals((byte)0, tProps.getValue("Orientation"));
      assertEquals(Boolean.FALSE, tProps.getValue("OrderByOn"));
      assertEquals((byte)2, tProps.getValue("DefaultView"));

      PropertyMap colProps = t.getColumn("A").getProperties();
      assertEquals("A", colProps.getName());
      expectedNumProps = 9;
      if(db.getFileFormat() != Database.FileFormat.V1997) {
        assertEquals("{E9EDD90C-CE55-4151-ABE1-A1ACE1007515}", 
                     colProps.getValue(PropertyMap.GUID_PROP));
        ++expectedNumProps;
      }
      assertEquals(expectedNumProps, colProps.getSize());
      assertEquals((short)-1, colProps.getValue("ColumnWidth"));
      assertEquals((short)0, colProps.getValue("ColumnOrder"));
      assertEquals(Boolean.FALSE, colProps.getValue("ColumnHidden"));
      assertEquals(Boolean.FALSE, 
                   colProps.getValue(PropertyMap.REQUIRED_PROP));
      assertEquals(Boolean.FALSE, 
                   colProps.getValue(PropertyMap.ALLOW_ZERO_LEN_PROP));
      assertEquals((short)109, colProps.getValue("DisplayControl"));
      assertEquals(Boolean.TRUE, colProps.getValue("UnicodeCompression"));
      assertEquals((byte)0, colProps.getValue("IMEMode"));
      assertEquals((byte)3, colProps.getValue("IMESentenceMode"));

      PropertyMap dbProps = db.getDatabaseProperties();
      assertTrue(((String)dbProps.getValue(PropertyMap.ACCESS_VERSION_PROP))
                 .matches("[0-9]{2}[.][0-9]{2}"));

      PropertyMap sumProps = db.getSummaryProperties();
      assertEquals(3, sumProps.getSize());
      assertEquals("test", sumProps.getValue(PropertyMap.TITLE_PROP));
      assertEquals("tmccune", sumProps.getValue(PropertyMap.AUTHOR_PROP));
      assertEquals("Health Market Science", sumProps.getValue(PropertyMap.COMPANY_PROP));

      PropertyMap userProps = db.getUserDefinedProperties();
      assertEquals(1, userProps.getSize());
      assertEquals(Boolean.TRUE, userProps.getValue("ReplicateProject"));

      db.close();
    }
  }

  public void testParseProperties() throws Exception
  {
    for(FileFormat ff : SUPPORTED_FILEFORMATS_FOR_READ) {
      File[] dbFiles = new File(DIR_TEST_DATA, ff.name()).listFiles();
      if(dbFiles == null) {
        continue;
      }
      for(File f : dbFiles) {

        if(!f.isFile()) {
          continue;
        }

        Database db = open(ff, f);

        PropertyMap dbProps = db.getDatabaseProperties();
        assertFalse(dbProps.isEmpty());
        assertTrue(((String)dbProps.getValue(PropertyMap.ACCESS_VERSION_PROP))
                   .matches("[0-9]{2}[.][0-9]{2}"));

        for(Map<String,Object> row : ((DatabaseImpl)db).getSystemCatalog()) {
          int id = (Integer)row.get("Id");
          byte[] propBytes = (byte[])row.get("LvProp");
          PropertyMaps propMaps = ((DatabaseImpl)db).getPropertiesForObject(id);
          int byteLen = ((propBytes != null) ? propBytes.length : 0);
          if(byteLen == 0) {
            assertTrue(propMaps.isEmpty());
          } else if(propMaps.isEmpty()) {
            assertTrue(byteLen < 80);
          } else {
            assertTrue(byteLen > 0);
          }
        }

        db.close();
      }
    }
  }

}