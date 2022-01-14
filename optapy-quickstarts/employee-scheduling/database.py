import datetime
import enum
import inspect
import sqlite3 as db

DATABASE_LOCATION = 'db.sqlite3'
entity_class_to_id_to_object_map = dict()


def entity(entity_class: type):
    entity_class.__database_entity_marker = True
    class_variables = inspect.signature(entity_class.__init__)
    type_annotations = entity_class.__init__.__annotations__
    entity_columns = list(class_variables.parameters.keys())
    entity_columns.remove('self')
    entity_columns_types = []
    foreign_columns = set()
    for table_column in entity_columns:
        if table_column not in type_annotations:
            raise TypeError(f'Parameter ({table_column}) does not have a type hint in __init__ for class '
                            f'({entity_class.__name__}). Maybe add a type hint (i.e. '
                            f'__init__(self, {table_column}: type, ...)?')
        table_column_type = type_annotations.get(table_column)
        entity_columns_types.append(table_column_type)
        if hasattr(table_column_type, '__database_entity_marker'):
            foreign_columns.add(table_column)

    table_name = entity_class.__name__

    def get_database_column_type(column_name):
        column_type = type_annotations.get(column_name)
        if column_type == int or hasattr(column_type, '__database_entity_marker'):
            return 'int'
        elif column_type == str or issubclass(column_type, enum.Enum) or column_type == list or \
                column_type == datetime.date or column_type == datetime.datetime:
            return 'text'
        else:
            raise TypeError(f'Column ({column_name}) has an unrecognized type ({column_type}). Maybe '
                            f'add the decorator @entity to the type ({column_type})?')

    def get_database_value(column_name, column_type, column_value):
        if column_value is None:
            return 'NULL'
        if column_name in foreign_columns:
            return int(column_value.id)
        if column_type == int:
            return int(column_value)
        if column_type == str:
            return str(column_value)
        if column_type == datetime.date:
            return column_value.isoformat()
        if column_type == datetime.datetime:
            return column_value.isoformat()
        if issubclass(column_type, enum.Enum):
            return column_value.value
        if column_type == list:
            return repr(column_value)
        raise TypeError(f'Unknown Type: {column_type}')

    def get_python_value(column_name, column_type, column_value):
        if column_value is None:
            return None
        if column_name in foreign_columns:
            return column_type.find_by_id(column_value)
        if column_type == int:
            return column_value
        if column_type == str:
            return column_value
        if column_type == datetime.date:
            return datetime.date.fromisoformat(column_value)
        if column_type == datetime.datetime:
            return datetime.datetime.fromisoformat(column_value)
        if issubclass(column_type, enum.Enum):
            return column_type(column_value)
        if column_type == list:
            return eval(column_value)
        raise TypeError(f'Unknown Type: {column_type}')

    def get_entity_columns(self):
        return tuple(map(lambda column: get_database_value(column, type_annotations.get(column),
                                                           getattr(self, column, None)), entity_columns))

    entity_class_to_id_to_object_map[entity_class] = dict()
    with db.connect(DATABASE_LOCATION) as create_table_connection:
        cur = create_table_connection.cursor()
        # Create table
        cur.execute(f"""DROP TABLE IF EXISTS {table_name}""")
        cur.execute(f"""CREATE TABLE {table_name}
               ({', '.join(map(lambda column: column + ' ' + get_database_column_type(column), entity_columns))},
               rowid INTEGER NOT NULL PRIMARY KEY)
               """)
        # Save (commit) the changes
        create_table_connection.commit()

    def insert(self):
        with db.connect(DATABASE_LOCATION) as connection:
            row_id = connection.execute(f"""
                INSERT INTO {table_name} ({', '.join(entity_columns)})
                values ({', '.join(map(lambda column: '?', entity_columns))}) 
            """, get_entity_columns(self)).lastrowid
            self.id = row_id
            entity_class_to_id_to_object_map.get(entity_class)[row_id] = self
            connection.commit()

    def update(self):
        active_instance = entity_class.find_by_id(self.id)
        if self is not active_instance:
            for column in entity_columns:
                setattr(active_instance, column, getattr(self, column, None))
        with db.connect(DATABASE_LOCATION) as connection:
            connection.execute(f"""
            UPDATE {table_name}
            SET
            {', '.join(map(lambda column: column + ' = ?', entity_columns))}
            WHERE {table_name}.rowid = ?;
            """, (*get_entity_columns(self), self.id))
            connection.commit()

    def save(self):
        if hasattr(self, 'id'):
            update(self)
        else:
            insert(self)

    entity_class.insert = insert
    entity_class.update = update
    entity_class.save = save

    def delete(self):
        del entity_class_to_id_to_object_map.get(entity_class)[self.id]
        with db.connect(DATABASE_LOCATION) as connection:
            connection.execute(f"""
            DELETE FROM {table_name}
            WHERE {table_name}.rowid = ?;
            """, (self.id, ))
            connection.commit()

    def delete_all():
        entity_class_to_id_to_object_map.get(entity_class).clear()
        with db.connect(DATABASE_LOCATION) as connection:
            connection.execute(f"""
            DELETE FROM {table_name}
            """)
            connection.commit()

    entity_class.delete_all = delete_all
    entity_class.delete = delete

    def row_to_entity(row):
        if row is None:
            return None
        entity_id = row[-1]
        if entity_id in entity_class_to_id_to_object_map.get(entity_class):
            return entity_class_to_id_to_object_map.get(entity_class).get(entity_id)
        init_parameters = []
        for i in range(len(entity_columns)):
            column_name = entity_columns[i]
            column_type = entity_columns_types[i]
            column_value = row[i]
            init_parameters.append(get_python_value(column_name, column_type, column_value))
        out = entity_class(*init_parameters)
        out.id = entity_id
        entity_class_to_id_to_object_map.get(entity_class)[entity_id] = out
        return out

    def find_by_id(entity_id):
        if entity_id in entity_class_to_id_to_object_map.get(entity_class):
            return entity_class_to_id_to_object_map.get(entity_class).get(entity_id)
        with db.connect(DATABASE_LOCATION) as connection:
            row = connection.execute(f"""
            SELECT * FROM {table_name} WHERE {table_name}.rowid = ?
            """, (entity_id,)).fetchone()
            return row_to_entity(row)

    def find_all():
        with db.connect(DATABASE_LOCATION) as connection:
            rows = connection.execute(f"""
            SELECT * FROM {table_name}
            """).fetchall()
            return list(map(row_to_entity, rows))

    entity_class.find_all = find_all
    entity_class.find_by_id = find_by_id

    def to_dict(self):
        out = dict()
        for column_name in entity_columns:
            value = getattr(self, column_name)
            if column_name in foreign_columns:
                if value is not None:
                    out[column_name] = value.to_dict()
                else:
                    out[column_name] = None
            elif isinstance(value, datetime.date) or isinstance(value, datetime.datetime):
                out[column_name] = value.isoformat()
            elif isinstance(value, enum.Enum):
                out[column_name] = value.value
            else:
                out[column_name] = value
        if hasattr(self, 'id'):
            out['id'] = self.id
        return out

    entity_class.to_dict = to_dict
    return entity_class
