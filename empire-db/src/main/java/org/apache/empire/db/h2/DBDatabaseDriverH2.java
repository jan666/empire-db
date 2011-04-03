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
package org.apache.empire.db.h2;

import java.sql.Connection;
import java.util.GregorianCalendar;
import java.util.Iterator;

import org.apache.empire.EmpireException;
import org.apache.empire.commons.Errors;
import org.apache.empire.commons.StringUtils;
import org.apache.empire.data.DataType;
import org.apache.empire.db.DBCmdType;
import org.apache.empire.db.DBColumn;
import org.apache.empire.db.DBCommand;
import org.apache.empire.db.DBCommandExpr;
import org.apache.empire.db.DBDatabase;
import org.apache.empire.db.DBDatabaseDriver;
import org.apache.empire.db.DBDriverFeature;
import org.apache.empire.db.DBExpr;
import org.apache.empire.db.DBIndex;
import org.apache.empire.db.DBObject;
import org.apache.empire.db.DBRelation;
import org.apache.empire.db.DBSQLScript;
import org.apache.empire.db.DBTable;
import org.apache.empire.db.DBTableColumn;
import org.apache.empire.db.DBView;


/**
 * This class provides support for the H2 database system.
 * 
 *
 */
public class DBDatabaseDriverH2 extends DBDatabaseDriver
{

  private final static long serialVersionUID = 1L;

    /**
     * Defines the H2 command type.
     */ 
    public static class DBCommandH2 extends DBCommand
    {
        private final static long serialVersionUID = 1L;
      
        public DBCommandH2(DBDatabase db)
        {
            super(db);
        }
    }
    
    // Properties
    private String databaseName = null;
    // Sequence treatment
    // When set to 'false' (default) H2's autoincrement feature is used.
    private boolean useSequenceTable = false;
    private String sequenceTableName = "Sequences";
    
    /**
     * Constructor for the H2 database driver.<br>
     */
    public DBDatabaseDriverH2()
    {
        // Default Constructor
    }

    /**
     * returns the name for the database / schema
     * @return the database / schema name
     */
    public String getDatabaseName()
    {
        return databaseName;
    }

    /**
     * Sets the name for the database / schema<br>
     * This names is required for creating a database.<br>
     * When a name is set, the driver will automatically execute 'USE dbname' when the database is opened.
     * @param databaseName the name of the database
     */
    public void setDatabaseName(String databaseName)
    {
        this.databaseName = databaseName;
    }

    /**
     * returns whether a sequence table is used for record identiy management.<br>
     * Default is false. In this case the AutoIncrement feature of H2 is used.
     * @return true if a sequence table is used instead of identity columns.
     */
    public boolean isUseSequenceTable()
    {
        return useSequenceTable;
    }

    /**
     * If set to true a special table is used for sequence number generation.<br>
     * Otherwise the AutoIncrement feature of H2 is used identiy fields. 
     * @param useSequenceTable true to use a sequence table or false otherwise.
     */
    public void setUseSequenceTable(boolean useSequenceTable)
    {
        this.useSequenceTable = useSequenceTable;
    }

    /**
     * returns the name of the sequence table
     * @return the name of the table used for sequence number generation
     */
    public String getSequenceTableName()
    {
        return sequenceTableName;
    }

    /**
     * Sets the name of the sequence table.
     * Only applicable if useSequenceTable is set to true.
     * @param sequenceTableName the name of the table used for sequence number generation
     */
    public void setSequenceTableName(String sequenceTableName)
    {
        this.sequenceTableName = sequenceTableName;
    }

    /**
     * Creates a new H2 command object.
     * 
     * @return the new DBCommandDerby object
     */
    @Override
    public DBCommand createCommand(DBDatabase db)
    {
        if (db == null)
            return null;
        // create command object
        return new DBCommandH2(db);
    }

    /**
     * Returns whether or not a particular feature is supported by this driver
     * @param type type of requrested feature. @see DBDriverFeature
     * @return true if the features is supported or false otherwise
     */
    @Override
    public boolean isSupported(DBDriverFeature type)
    {
        switch (type)
        {   // return support info 
            case CREATE_SCHEMA: return true;
            case SEQUENCES:     return useSequenceTable;    
            default:
                // All other features are not supported by default
                return false;
        }
    }
    
    /**
     * Gets an sql phrase template for this database system.<br>
     * @see DBDatabaseDriver#getSQLPhrase(int)
     * @return the phrase template
     */
    @Override
    public String getSQLPhrase(int phrase)
    {
        switch (phrase)
        {
            // sql-phrases
            case SQL_NULL_VALUE:              return "null";
            case SQL_PARAMETER:               return " ? ";
            case SQL_RENAME_TABLE:            return " ";
            case SQL_RENAME_COLUMN:           return " AS ";
            case SQL_DATABASE_LINK:           return "@";
            case SQL_QUOTES_OPEN:             return "\"";
            case SQL_QUOTES_CLOSE:            return "\"";
            case SQL_CONCAT_EXPR:             return "concat(?, {0})";
            // data types
            case SQL_BOOLEAN_TRUE:            return "1";
            case SQL_BOOLEAN_FALSE:           return "0";
            case SQL_CURRENT_DATE:            return "CURRENT_DATE()";
            case SQL_DATE_PATTERN:            return "yyyy-MM-dd";
            case SQL_DATE_TEMPLATE:           return "'{0}'";
            case SQL_CURRENT_DATETIME:        return "NOW()";
            case SQL_DATETIME_PATTERN:        return "yyyy-MM-dd HH:mm:ss";
            case SQL_DATETIME_TEMPLATE:       return "'{0}'";
            // functions
            case SQL_FUNC_COALESCE:           return "coalesce(?, {0})";
            case SQL_FUNC_SUBSTRING:          return "substring(?, {0})";
            case SQL_FUNC_SUBSTRINGEX:        return "substring(?, {0}, {1})";
            case SQL_FUNC_REPLACE:            return "replace(?, {0}, {1})";
            case SQL_FUNC_REVERSE:            return "reverse_not_available_in_h2(?)"; 
            case SQL_FUNC_STRINDEX:           return "instr(?, {0})"; 
            case SQL_FUNC_STRINDEXFROM:       return "locate({0}, ?, {1})"; 
            case SQL_FUNC_LENGTH:             return "length(?)";
            case SQL_FUNC_UPPER:              return "upper(?)";
            case SQL_FUNC_LOWER:              return "lcase(?)";
            case SQL_FUNC_TRIM:               return "trim(?)";
            case SQL_FUNC_LTRIM:              return "ltrim(?)";
            case SQL_FUNC_RTRIM:              return "rtrim(?)";
            case SQL_FUNC_ESCAPE:             return "? escape '{0}'";
            // Numeric
            case SQL_FUNC_ABS:                return "abs(?)";
            case SQL_FUNC_ROUND:              return "round(?,{0})";
            case SQL_FUNC_TRUNC:              return "truncate(?,{0})";
            case SQL_FUNC_CEILING:            return "ceiling(?)";
            case SQL_FUNC_FLOOR:              return "floor(?)";
            // Date
            case SQL_FUNC_DAY:                return "day(?)";
            case SQL_FUNC_MONTH:              return "month(?)";
            case SQL_FUNC_YEAR:               return "year(?)";
            // Aggregation
            case SQL_FUNC_SUM:                return "sum(?)";
            case SQL_FUNC_MAX:                return "max(?)";
            case SQL_FUNC_MIN:                return "min(?)";
            case SQL_FUNC_AVG:                return "avg(?)";
            // Others
            case SQL_FUNC_DECODE:             return "case ? {0} end";
            case SQL_FUNC_DECODE_SEP:         return " ";
            case SQL_FUNC_DECODE_PART:        return "when {0} then {1}";
            case SQL_FUNC_DECODE_ELSE:        return "else {0}";
            // Not defined
            default:
                log.error("SQL phrase " + phrase + " is not defined!");
                return "?";
        }
    }

    /**
     * @see DBDatabaseDriver#getConvertPhrase(DataType, DataType, Object)
     */
    @Override
    public String getConvertPhrase(DataType destType, DataType srcType, Object format)
    {
        switch(destType)
        {
           case BOOL:      return "CAST(? AS UNSIGNED)";
           case INTEGER:   return "CAST(? AS SIGNED)";
           case DECIMAL:   return "CAST(? AS DECIMAL)";
           case DOUBLE:    return "CAST(? AS DECIMAL)";
           case DATE:      return "CAST(? AS DATE)";
           case DATETIME:  return "CAST(? AS DATETIME)";
           // Convert to text
           case TEXT:
                return "CAST(? AS CHAR)";
           case BLOB:
                return "CAST(? AS BLOB)";
           // Unknown Type                                       
           default:
                log.error("getConvertPhrase: unknown type " + destType);
                return "?";
        }
    }
    
    /**
     * @see DBDatabaseDriver#getNextSequenceValue(DBDatabase, String, int, Connection)
     */
    @Override
    public Object getNextSequenceValue(DBDatabase db, String seqName, int minValue, Connection conn)
    {   //Use Oracle Sequences
        if (useSequenceTable)
        {   // Use a sequence Table to generate Sequences
            DBTable t = db.getTable(sequenceTableName);
            return ((DBSeqTable)t).getNextValue(seqName, minValue, conn);
        }
        else
        {   // Post Detection
            return null;
        }
    }

    /**
     * @see DBDatabaseDriver#getDDLScript(DBCmdType, DBObject, DBSQLScript)  
     */
    @Override
    public void getDDLScript(DBCmdType type, DBObject dbo, DBSQLScript script)
    {
        // The Object's database must be attached to this driver
        if (dbo==null || dbo.getDatabase().getDriver()!=this)
        	throw new EmpireException(Errors.InvalidArg, dbo, "dbo");
        // Check Type of object
        if (dbo instanceof DBDatabase)
        { // Database
            switch (type)
            {
                case CREATE:
                    createDatabase((DBDatabase) dbo, script, true);
                    break;
                case DROP:
                    dropObject(((DBDatabase) dbo).getSchema(), "DATABASE", script);
                    break;
                default:
                	throw new EmpireException(Errors.NotImplemented, "getDDLScript." + dbo.getClass().getName() + "." + type);
            }
        } 
        else if (dbo instanceof DBTable)
        { // Table
            switch (type)
            {
                case CREATE:
                    createTable((DBTable) dbo, script);
                    break;
                case DROP:
                    dropObject(((DBTable) dbo).getName(), "TABLE", script);
                    break;
                default:
                	throw new EmpireException(Errors.NotImplemented, "getDDLScript." + dbo.getClass().getName() + "." + type);
            }
        } 
        else if (dbo instanceof DBView)
        { // View
            switch (type)
            {
                case CREATE:
                    createView((DBView) dbo, script);
                case DROP:
                    dropObject(((DBView) dbo).getName(), "VIEW", script);
                default:
                	throw new EmpireException(Errors.NotImplemented, "getDDLScript." + dbo.getClass().getName() + "." + type);
            }
        } 
        else if (dbo instanceof DBRelation)
        { // Relation
            switch (type)
            {
                case CREATE:
                    createRelation((DBRelation) dbo, script);
                case DROP:
                    dropObject(((DBRelation) dbo).getName(), "CONSTRAINT", script);
                default:
                	throw new EmpireException(Errors.NotImplemented, "getDDLScript." + dbo.getClass().getName() + "." + type);
            }
        } 
        else if (dbo instanceof DBTableColumn)
        { // Table Column
            alterTable((DBTableColumn) dbo, type, script);
        } 
        else
        { // an invalid argument has been supplied
        	throw new EmpireException(Errors.InvalidArg, dbo, "dbo");
        }
    }

    /**
     * Overridden. Returns a timestamp that is used for record updates created by the database server.
     * 
     * @return the current date and time of the database server.
     */
    @Override
    public java.sql.Timestamp getUpdateTimestamp(Connection conn)
    {
        // Default implementation
        GregorianCalendar cal = new GregorianCalendar();
        return new java.sql.Timestamp(cal.getTimeInMillis());
    }

    /*
     * return the sql for creating a Database
     */
    protected void createDatabase(DBDatabase db, DBSQLScript script, boolean createSchema)
    {
//        // User Master to create Database
//        if (createSchema)
//        {   // check database Name
//            if (StringUtils.isValid(databaseName)==false)
//                return error(Errors.InvalidProperty, "databaseName");
//            // Create Database
//            script.addStmt("CREATE DATABASE " + databaseName + " CHARACTER SET " + characterSet);
//            script.addStmt("USE " + databaseName);
//            // appendDDLStmt(db, "SET DATEFORMAT ymd", buf);
//            // Sequence Table
//            if (useSequenceTable && db.getTable(sequenceTableName)==null)
//                new DBSeqTable(sequenceTableName, db);
//        }
        // Create all Tables
        Iterator<DBTable> tables = db.getTables().iterator();
        while (tables.hasNext())
        {
            createTable(tables.next(), script);
        }
        // Create Relations
        Iterator<DBRelation> relations = db.getRelations().iterator();
        while (relations.hasNext())
        {
            createRelation(relations.next(), script);
        }
        // Create Views
        Iterator<DBView> views = db.getViews().iterator();
        while (views.hasNext())
        {
            createView(views.next(), script);
        }
    }
    
    /**
     * Returns true if the table has been created successfully.
     * 
     * @return true if the table has been created successfully
     */
    protected void createTable(DBTable t, DBSQLScript script)
    {
        StringBuilder sql = new StringBuilder();
        sql.append("-- creating table ");
        sql.append(t.getName());
        sql.append(" --\r\n");
        sql.append("CREATE TABLE ");
        t.addSQL(sql, DBExpr.CTX_FULLNAME);
        sql.append(" (");
        boolean addSeparator = false;
        Iterator<DBColumn> columns = t.getColumns().iterator();
        while (columns.hasNext())
        {
            DBTableColumn c = (DBTableColumn) columns.next();
            sql.append((addSeparator) ? ",\r\n   " : "\r\n   ");
            if (appendColumnDesc(c, sql)==false)
                continue; // Ignore and continue;
            addSeparator = true;
        }
        // Primary Key
        DBIndex pk = t.getPrimaryKey();
        if (pk != null)
        { // add the primary key
            sql.append(", PRIMARY KEY (");
            addSeparator = false;
            // columns
            DBColumn[] keyColumns = pk.getColumns();
            for (int i = 0; i < keyColumns.length; i++)
            {
                sql.append((addSeparator) ? ", " : "");
                keyColumns[i].addSQL(sql, DBExpr.CTX_NAME);
                addSeparator = true;
            }
            sql.append(")");
        }
        sql.append(")");
        // Comment?
        String comment = t.getComment();
        if (StringUtils.isNotEmpty(comment))
        {   // Add the table comment
            sql.append(" COMMENT = '");
            sql.append(comment);
            sql.append("'");
        }
        // Create the table
        script.addStmt(sql);
        // Create other Indexes (except primary key)
        Iterator<DBIndex> indexes = t.getIndexes().iterator();
        while (indexes.hasNext())
        {
            DBIndex idx = indexes.next();
            if (idx == pk || idx.getType() == DBIndex.PRIMARYKEY)
                continue;

            // Cretae Index
            sql.setLength(0);
            sql.append((idx.getType() == DBIndex.UNIQUE) ? "CREATE UNIQUE INDEX " : "CREATE INDEX ");
            appendElementName(sql, idx.getName());
            sql.append(" ON ");
            t.addSQL(sql, DBExpr.CTX_FULLNAME);
            sql.append(" (");
            addSeparator = false;

            // columns
            DBColumn[] idxColumns = idx.getColumns();
            for (int i = 0; i < idxColumns.length; i++)
            {
                sql.append((addSeparator) ? ", " : "");
                idxColumns[i].addSQL(sql, DBExpr.CTX_NAME);
                addSeparator = true;
            }
            sql.append(")");
            // Create Index
            script.addStmt(sql);
        }
    }
    
    /**
     * Appends a table column definition to a ddl statement
     * @param c the column which description to append
     * @param sql the sql builder object
     * @return true if the column was successfully appended or false otherwise
     */
    protected boolean appendColumnDesc(DBTableColumn c, StringBuilder sql)
    {
        // Append name
        c.addSQL(sql, DBExpr.CTX_NAME);
        sql.append(" ");
        switch (c.getDataType())
        {
            case INTEGER:
            {
                int size = (int) c.getSize();
                if (size >= 8) {
                    sql.append("BIGINT");
                } else {
                    sql.append("INT");
                }
                break;
            }    
            case AUTOINC:
            { // Auto increment
                sql.append("INT");
                if (useSequenceTable==false)
                    sql.append(" AUTO_INCREMENT");
                break;
            }    
            case TEXT:
            { // Check fixed or variable length
                int size = Math.abs((int) c.getSize());
                if (size == 0)
                    size = 100;
                sql.append("VARCHAR(");
                sql.append(String.valueOf(size));
                sql.append(")");
            }
                break;
            case CHAR:
            { // Check fixed or variable length
                int size = Math.abs((int) c.getSize());
                if (size == 0)
                    size = 1;
                sql.append("CHAR(");
                sql.append(String.valueOf(size));
                sql.append(")");
            }
                break;
            case DATE:
                sql.append("DATE");
                break;
            case DATETIME:
                sql.append("DATETIME");
                break;
            case BOOL:
                sql.append("BIT");
                break;
            case DOUBLE:
                sql.append("DOUBLE");
                break;
            case DECIMAL:
            { // Decimal
                sql.append("DECIMAL(");
                int prec = (int) c.getSize();
                int scale = (int) ((c.getSize() - prec) * 10 + 0.5);
                // sql.append((prec+scale).ToString());sql.append(",");
                sql.append(String.valueOf(prec));
                sql.append(",");
                sql.append(String.valueOf(scale));
                sql.append(")");
            }
                break;
            case CLOB:
                sql.append("LONGTEXT");
                break;
            case BLOB:
                sql.append("BLOB");
                if (c.getSize() > 0)
                    sql.append(" (" + (long) c.getSize() + ") ");
                break;
            case UNIQUEID:
                // emulate using java.util.UUID
                sql.append("CHAR(36)");
                break;
            case UNKNOWN:
                 log.error("Cannot append column of Data-Type 'UNKNOWN'");
                 return false;
        }
        // Default Value
        if (isDDLColumnDefaults() && !c.isAutoGenerated() && c.getDefaultValue()!=null)
        {   sql.append(" DEFAULT ");
            sql.append(getValueString(c.getDefaultValue(), c.getDataType()));
        }
        // Nullable
        if (c.isRequired() ||  c.isAutoGenerated())
            sql.append(" NOT NULL");
        // Done
        return true;
    }

    /**
     * Returns true if the relation has been created successfully.
     * 
     * @return true if the relation has been created successfully
     */
    protected void createRelation(DBRelation r, DBSQLScript script)
    {
        DBTable sourceTable = (DBTable) r.getReferences()[0].getSourceColumn().getRowSet();
        DBTable targetTable = (DBTable) r.getReferences()[0].getTargetColumn().getRowSet();

        StringBuilder sql = new StringBuilder();
        sql.append("-- creating foreign key constraint ");
        sql.append(r.getName());
        sql.append(" --\r\n");
        sql.append("ALTER TABLE ");
        sourceTable.addSQL(sql, DBExpr.CTX_FULLNAME);
        sql.append(" ADD CONSTRAINT ");
        appendElementName(sql, r.getName());
        sql.append(" FOREIGN KEY (");
        // Source Names
        boolean addSeparator = false;
        DBRelation.DBReference[] refs = r.getReferences();
        for (int i = 0; i < refs.length; i++)
        {
            sql.append((addSeparator) ? ", " : "");
            refs[i].getSourceColumn().addSQL(sql, DBExpr.CTX_NAME);
            addSeparator = true;
        }
        // References
        sql.append(") REFERENCES ");
        targetTable.addSQL(sql, DBExpr.CTX_FULLNAME);
        sql.append(" (");
        // Target Names
        addSeparator = false;
        for (int i = 0; i < refs.length; i++)
        {
            sql.append((addSeparator) ? ", " : "");
            refs[i].getTargetColumn().addSQL(sql, DBExpr.CTX_NAME);
            addSeparator = true;
        }
        // done
        sql.append(")");
        script.addStmt(sql);
    }

    /**
     * Creates an alter table dll statement for adding, modifiying or droping a column.
     * @param col the column which to add, modify or drop
     * @param type the type of operation to perform
     * @param script to which to append the sql statement to
     * @return true if the statement was successfully appended to the buffer
     */
    protected void alterTable(DBTableColumn col, DBCmdType type, DBSQLScript script)
    {
        StringBuilder sql = new StringBuilder();
        sql.append("ALTER TABLE ");
        col.getRowSet().addSQL(sql, DBExpr.CTX_FULLNAME);
        switch(type)
        {
            case CREATE:
                sql.append(" ADD ");
                appendColumnDesc(col, sql);
                break;
            case ALTER:
                sql.append(" ALTER ");
                appendColumnDesc(col, sql);
                break;
            case DROP:
                sql.append(" DROP COLUMN ");
                sql.append(col.getName());
                break;
        }
        // done
        script.addStmt(sql);
    }

    /**
     * Returns true if the view has been created successfully.
     * 
     * @return true if the view has been created successfully
     */
    protected void createView(DBView v, DBSQLScript script)
    {
        // Create the Command
        DBCommandExpr cmd = v.createCommand();
        if (cmd==null)
        {   // Check whether Error information is available
            log.error("No command has been supplied for view " + v.getName());
            // No error information available: Use Errors.NotImplemented
            throw new EmpireException(Errors.NotImplemented, v.getName() + ".createCommand");
        }
        // Make sure there is no OrderBy
        cmd.clearOrderBy();

        // Build String
        StringBuilder sql = new StringBuilder();
        sql.append( "CREATE VIEW ");
        v.addSQL(sql, DBExpr.CTX_FULLNAME);
        sql.append( " (" );
        boolean addSeparator = false;
        for(DBColumn c : v.getColumns())
        {
            if (addSeparator)
                sql.append(", ");
            // Add Column name
            c.addSQL(sql, DBExpr.CTX_NAME);
            // next
            addSeparator = true;
        }
        sql.append(")\r\nAS\r\n");
        cmd.addSQL( sql, DBExpr.CTX_DEFAULT);
        // done
        script.addStmt(sql.toString());
    }
    
    /**
     * Returns true if the object has been dropped successfully.
     * 
     * @return true if the object has been dropped successfully
     */
    protected void dropObject(String name, String objType, DBSQLScript script)
    {
        if (name == null || name.length() == 0)
        	throw new EmpireException(Errors.InvalidArg, name, "name");
        // Create Drop Statement
        StringBuilder sql = new StringBuilder();
        sql.append("DROP ");
        sql.append(objType);
        sql.append(" ");
        appendElementName(sql, name);
        script.addStmt(sql);
    }

}
