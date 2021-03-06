/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *  http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */
package org.apache.empire.db;

import java.math.BigDecimal;
import java.sql.Connection;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;

import org.apache.empire.commons.Attributes;
import org.apache.empire.commons.ObjectUtils;
import org.apache.empire.commons.StringUtils;
import org.apache.empire.data.DataMode;
import org.apache.empire.data.DataType;
import org.apache.empire.db.exceptions.FieldIllegalValueException;
import org.apache.empire.db.exceptions.FieldNotNullException;
import org.apache.empire.db.exceptions.FieldValueOutOfRangeException;
import org.apache.empire.db.exceptions.FieldValueTooLongException;
import org.apache.empire.exceptions.InvalidArgumentException;
import org.apache.empire.exceptions.NotSupportedException;
import org.apache.empire.exceptions.PropertyReadOnlyException;
import org.apache.empire.xml.XMLUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.w3c.dom.Element;


/**
 * This class represent one column of a table.
 * It contains all properties of this columns (e.g. the column width).
 * 
 *
 */
public class DBTableColumn extends DBColumn
{
    private final static long serialVersionUID = 1L;
    private static final Logger log = LoggerFactory.getLogger(DBTableColumn.class);
    
    // Column Information
    protected DataType  type;
    protected double    size;
    protected DataMode  dataMode;
    protected Object    defValue;
    protected int 		decimalScale = 0;

    /**
     * Constructs a DBTableColumn object set the specified parameters to this object.
     * 
     * @param table the table object to add the column to, set to null if you don't want it added to a table
     * @param type the type of the column e.g. integer, text, date
     * @param name the column name
     * @param size the column width
     * @param dataMode determines whether this column is optional, required or auto-generated 
     * @param defValue the object value
     */
    public DBTableColumn(DBTable table, DataType type, String name, double size, DataMode dataMode, Object defValue)
    {
        super(table, name);
        // check properties
        // Make sure (DataType.INTEGER & DataMode.AutoGenerated) = DataType.AUTOINC
        if (type==DataType.AUTOINC && dataMode!=DataMode.AutoGenerated)
            dataMode=DataMode.AutoGenerated;
        if (type==DataType.INTEGER && dataMode==DataMode.AutoGenerated)
            type=DataType.AUTOINC;           
        // set column properties
        this.type = type;
        this.dataMode = dataMode;
        this.defValue = defValue;
        // xml
        this.attributes = new Attributes();
        this.options = null;
        // size (after attributes!)
        setSize(size);
        // Append to table (if supplied)
        if (table != null)
        {
            table.addColumn(this);
        }
    }

    /**
     * Clone Constructor - use clone()
     */
    protected DBTableColumn(DBTable newTable, DBTableColumn other)
    {
        super(newTable, other.name);
        // Copy
        this.type = other.type;
        this.size = other.size;
        this.dataMode = other.dataMode;
        this.defValue = other.defValue;
        this.attributes = new Attributes();
        this.attributes.addAll(other.attributes);
        this.options = other.options;
        if (newTable != null)
        {
            newTable.addColumn(this);
        }
    }
    
    /**
     * Returns the default column value.
     * For columns of type DBDataType.AUTOINC this is assumed to be the name of a sequence
     * 
     * @return the default column value
     */
    public Object getDefaultValue()
    {
        return defValue;
    }

    /**
     * Sets the default column value.
     * 
     * @param defValue the default column value
     */
    public void setDefaultValue(Object defValue)
    {
        this.defValue = defValue;
    }
    
    /**
     * Returns the default column value. 
     * Unlike getDefaultValue this function is used when creating or adding records.
     * If the column value is DBDataType AUTOIN this function will return a new sequence value for this record
     * 
     * @param conn a valid database connection
     * @return the default column value
     */
    public Object getRecordDefaultValue(Connection conn)
    {	// Check params   
        if (rowset==null)
            return defValue;
        // Detect default value
        DBDatabase db = rowset.getDatabase();
        if (isAutoGenerated())
        {   // If no connection is supplied defer till later
        	if (conn==null)
        		return null; // Create Later
            // Other auto-generated values
        	DBDatabaseDriver driver = db.getDriver();
            return driver.getColumnAutoValue(db, this, conn);
        }
        // Normal value
        return defValue;
    }

    /**
     * Returns the data type.
     * 
     * @return the data type
     */
    @Override
    public DataType getDataType()
    {
        return type;
    }

    /**
     * Gets the the column width.
     * 
     * @return the column width
     */
    @Override
    public double getSize()
    {
        return size;
    }

    /**
     * Changes the size of the table column<BR>
     * Use for dynamic data model changes only.<BR>
     * @param size the new column size
     */
    public void setSize(double size)
    {
        // Negative size?
        if (size<0)
        {   // For Text-Columns set attribute "SINGLEBYTECHARS"
            if (getDataType().isText())
            {
                setAttribute(DBCOLATTR_SINGLEBYTECHARS, Boolean.TRUE);
            }    
            else
                throw new InvalidArgumentException("size", size);
            // Remove sign
            size = Math.abs(size);
        }
        else if (attributes!=null && attributes.contains(DBCOLATTR_SINGLEBYTECHARS))
        {   // Remove single by chars attribute
            attributes.remove(DBCOLATTR_SINGLEBYTECHARS);
        }
        // set now
        this.size = size;
        // set scale
    	if (getDataType()==DataType.DECIMAL)
    	{	// set scale from size
		    int reqPrec = (int)size;
		    this.decimalScale = ((int)(size*10)-(reqPrec*10));
    	}
    }
    
    /**
     * Returns the scale of the Decimal or 0 if the DataType is not DataType.DECIMAL.
     * @return the decimal scale
     */
    public int getDecimalScale()
    {
	    return this.decimalScale;
    }
    
    /**
     * Sets the scale of a decimal. The DataType must be set to DataType.DECIMAL otherwise an exception is thrown. 
     */
    public void setDecimalScale(int scale)
    {
    	if (getDataType()!=DataType.DECIMAL)
    		throw new NotSupportedException(this, "setDecimalScale");
    	// return scale
	    this.decimalScale = scale;
    }

    /**
     * Returns true if column is mandatory. Only for the graphic presentation.
     * 
     * @return true if column is mandatory 
     */
    @Override
    public boolean isRequired()
    {
        return (dataMode==DataMode.NotNull);
    }
    
    /**
     * Returns true if column is a numeric sequence or otherwise generated value
     * 
     * @return true if column is auto increment
     */
    @Override
    public boolean isAutoGenerated()
    {
        return (dataMode==DataMode.AutoGenerated);
    }
    
    /**
     * Returns true if column the column is a single byte text or character column or false otherwise
     * 
     * @return true if column is a single byte character based column
     */
    public boolean isSingleByteChars()
    {
        if (attributes==null || !attributes.contains(DBCOLATTR_SINGLEBYTECHARS))
            return false;
        // Check Attribute
        return ObjectUtils.getBoolean(attributes.get(DBCOLATTR_SINGLEBYTECHARS));
    }
    
    /**
     * sets whether this column is a single byte character or text column 
     */
    public void setSingleByteChars(boolean singleByteChars)
    {
        if (!getDataType().isText())
            throw new NotSupportedException(this, "setSingleByteChars");
        // set single byte
        setAttribute(DBCOLATTR_SINGLEBYTECHARS, singleByteChars);
    }

    /**
     * Changes the required property of the table column<BR>
     * Use for dynamic data model changes only.<BR>
     * @param required true if the column is required or false otherwise
     */
    public void setRequired(boolean required)
    {
        if (isAutoGenerated())
        {	// cannot change auto-generated columns
            throw new PropertyReadOnlyException("required"); 
        }
        else
        {	// Set DataMode
        	dataMode=(required ? DataMode.NotNull : DataMode.Nullable);
        }
    }

    /**
     * Checks whether the column is read only.
     * 
     * @return true if the column is read only
     */
    @Override
    public boolean isReadOnly()
    {
        if (attributes!=null &&
            attributes.contains(DBCOLATTR_READONLY))
            return true;
        // Check DataMode
        return (dataMode==DataMode.ReadOnly || dataMode==DataMode.AutoGenerated);
    }

    /**
     * Sets the read only attribute of the column.
     *
     * @param readOnly true if the column should be read only or false otherwise
     */
    public void setReadOnly(boolean readOnly)
    {
        if (readOnly)
        {
            setAttribute(DBCOLATTR_READONLY, Boolean.TRUE);
        }
        else  
        {   // Remove Attribute
            if (attributes!=null)
                attributes.remove(DBCOLATTR_READONLY);
        }
    }

    /**
     * Checks whether the supplied value is valid for this column.
     * If the type of the value supplied does not match the columns
     * data type the value will be checked for compatibility. 
     * 
     * @param value the checked to check for validity
     * @return true if the value is valid or false otherwise.
     */
    @Override
    public Object validate(Object value)
    {
        // Check for NULL
        if (isRequired() && ObjectUtils.isEmpty(value))
            throw new FieldNotNullException(this);
        // Is value valid
        switch (type)
        {
            case DATE:
            case DATETIME:
                // Check whether value is a valid date/time value!
                if (value!=null && !(value instanceof Date) && !DBDatabase.SYSDATE.equals(value))
                {   // Parse String
                    String dateValue = value.toString();
                    if (dateValue.length()==0)
                        return null;
                    // Convert through SimpleDateFormat
                    String datePattern = StringUtils.coalesce(StringUtils.toString(getAttribute(DBCOLATTR_DATETIMEPATTERN)), "yyyy-MM-dd HH:mm:ss");
                    if ((type==DataType.DATE || dateValue.length()<=12) && datePattern.indexOf(' ')>0)
                        datePattern = datePattern.substring(0, datePattern.indexOf(' ')); // Strip off time
                    try
                    { 	// Parse date time value
                        SimpleDateFormat sdFormat = new SimpleDateFormat(datePattern);
                        sdFormat.setLenient(true);
                        value = sdFormat.parse(dateValue);
                        // OK
                    } catch (ParseException e)
                    {   // Error
                        log.info("Parsing '{}' to Date ("+datePattern+") failed for column {}. Message is "+e.toString(), value, getName());
                        throw new FieldIllegalValueException(this, String.valueOf(value), e);
                    }
                }    
                break;

            case DECIMAL:
                if (value==null)
                    break;
                if (!(value instanceof java.lang.Number))
                {   try
                    {   // Convert to String and check
                        value = ObjectUtils.toDecimal(value);
                        // throws NumberFormatException if not a number!
                    } catch (NumberFormatException e)
                    {
                        log.info("Parsing '{}' to Decimal failed for column {}. Message is "+e.toString(), value, getName());
                        throw new FieldIllegalValueException(this, String.valueOf(value), e);
                    }
                }
                // validate Number
                validateNumber(type, (Number)value);
                break;

            case FLOAT:
                if (value==null)
                    break;
                if (!(value instanceof java.lang.Number))
                {   try
                    {   // Convert to String and check
                        value = ObjectUtils.toDouble(value);
                        // throws NumberFormatException if not a number!
                    } catch (NumberFormatException e)
                    {
                        log.info("Parsing '{}' to Double failed for column {}. Message is "+e.toString(), value, getName());
                        throw new FieldIllegalValueException(this, String.valueOf(value), e);
                    }
                }
                // validate Number
                validateNumber(type, (Number)value);
                break;

            case INTEGER:
                if (value==null)
                    break;
                if (!(value instanceof java.lang.Number))
                {   try
                    {   // Convert to String and check
                        value = ObjectUtils.toLong(value);
                        // throws NumberFormatException if not an integer!
                    } catch (NumberFormatException e)
                    {
                        log.info("Parsing '{}' to Integer failed for column {}. Message is "+e.toString(), value, getName());
                        throw new FieldIllegalValueException(this, String.valueOf(value), e);
                    }
                }
                // validate Number
                validateNumber(type, (Number)value);
                break;

            case TEXT:
            case CHAR:
                if (value!=null && value.toString().length() > size)
                    throw new FieldValueTooLongException(this);
                break;
                
            default:
                if (log.isDebugEnabled())
                    log.debug("No column validation has been implemented for data type " + type);
                break;

        }
        return value;
    }
    
    protected void validateNumber(DataType type, Number n)
    {
        // Check Range
        Object min = getAttribute(DBColumn.DBCOLATTR_MINVALUE);
        Object max = getAttribute(DBColumn.DBCOLATTR_MAXVALUE);
        if (min!=null && max!=null)
        {   // Check Range
            long minVal = ObjectUtils.getLong(min);
            long maxVal = ObjectUtils.getLong(max);
            if (n.longValue()<minVal || n.longValue()>maxVal)
            {   // Out of Range
                throw new FieldValueOutOfRangeException(this, minVal, maxVal);
            }
        }
        else if (min!=null)
        {   // Check Min Value
            long minVal = ObjectUtils.getLong(min);
            if (n.longValue()<minVal)
            {   // Out of Range
                throw new FieldValueOutOfRangeException(this, minVal, false);
            }
        }
        else if (max!=null)
        {   // Check Max Value
            long maxVal = ObjectUtils.getLong(max);
            if (n.longValue()>maxVal)
            {   // Out of Range
                throw new FieldValueOutOfRangeException(this, maxVal, true);
            }
        }
        // Check overall
        if (type==DataType.DECIMAL)
        {   // Convert to Decimal
            BigDecimal dv = ObjectUtils.toDecimal(n);
            int prec = dv.precision();
            int scale = dv.scale();
            // check precision and scale
            double size = getSize();
            int reqPrec = (int)size;
            int reqScale = getDecimalScale();
            if ((prec-scale)>(reqPrec-reqScale) || scale>reqScale)
                throw new FieldValueOutOfRangeException(this);
        }
    }

    /**
     * Creates a foreign key relation for this column.
     * 
     * @param target the referenced primary key column
     * @return the reference object
     */
    public DBRelation.DBReference referenceOn(DBTableColumn target)
    {
        return new DBRelation.DBReference(this, target);
    }

    /**
     * Sets field elements, default attributes and all options to
     * the specified Element object (XML tag).
     * 
     * @param parent the parent object
     * @param flags a long value
     * @return the work up Element object
     */
    @Override
    public Element addXml(Element parent, long flags)
    { // Add Field element
        Element elem = XMLUtil.addElement(parent, "column");
        elem.setAttribute("name", name);
        // set default attributes
        DBIndex primaryKey = ((DBTable) rowset).getPrimaryKey();
        if (primaryKey != null)
        {
            int keyIndex;
            if ((keyIndex = ((DBTable) rowset).getPrimaryKey().getColumnPos(this)) >= 0)
                elem.setAttribute("key", String.valueOf(keyIndex + 1));
        }
        if (size > 0)
        {
            elem.setAttribute("size", String.valueOf((int)size));
            if (getDataType()==DataType.DECIMAL)
                elem.setAttribute("decimals", String.valueOf((int)(size*10)%10));
        }
        if (isRequired())
            elem.setAttribute(DBCOLATTR_MANDATORY, String.valueOf(Boolean.TRUE));
        // add All Attributes
        if (attributes!=null)
            attributes.addXml(elem, flags);
        // add All Options
        if (options!=null)
            options.addXml(elem, flags);
        // done
        return elem;
    }
    
    /**
     * Gets the sequence name for this table's sequence (if it has one)
     * This is derived form the default value or auto generated if no default value is set
     * @return the sequence name
     */
	public String getSequenceName()
	{
		String seqName;
		Object defValue = getDefaultValue();
		if(defValue != null)
		{
			seqName = defValue.toString();
		}
		else
		{
			if (rowset != null) 
			{
				seqName = rowset.getName() + "." + name;
			} 
			else 
			{
				seqName = name;
			}
		}
		return seqName;
	}
}