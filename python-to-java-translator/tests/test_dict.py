from .conftest import verifier_for


def test_membership():
    membership_verifier = verifier_for(lambda tested, x: x in tested)
    not_membership_verifier = verifier_for(lambda tested, x: x not in tested)

    membership_verifier.verify({
        1: 'a',
        2: 'b'
    }, 1, expected_result=True)
    not_membership_verifier.verify({
        1: 'a',
        2: 'b'
    }, 1, expected_result=False)

    membership_verifier.verify({
        1: 'a',
        2: 'b'
    }, 3, expected_result=False)
    not_membership_verifier.verify({
        1: 'a',
        2: 'b'
    }, 3, expected_result=True)

    membership_verifier.verify({
        'a': 1,
        'b': 2
    }, 1, expected_result=False)
    not_membership_verifier.verify({
        'a': 1,
        'b': 2
    }, 3, expected_result=True)


def test_iter():
    def to_list(x):
        return list(x)

    to_list_verifier = verifier_for(to_list)

    to_list_verifier.verify(dict(), expected_result=[])
    to_list_verifier.verify({
        1: 'a',
        2: 'b'
    }, expected_result=[1, 2])
    to_list_verifier.verify({
        'a': 1,
        'b': 2
    }, expected_result=['a', 'b'])
    to_list_verifier.verify({
        3: 'a',
        2: 'b',
        1: 'c'
    }, expected_result=[3, 2, 1])


def test_get_item():
    get_item_verifier = verifier_for(lambda my_dict, key: my_dict[key])

    get_item_verifier.verify({
        1: 'a',
        2: 'b'
    }, 1, expected_result='a')
    get_item_verifier.verify({
        1: 'a',
        2: 'b'
    }, 2, expected_result='b')
    get_item_verifier.verify({
        1: 'a',
        2: 'b'
    }, 'a', expected_error=KeyError)
    get_item_verifier.verify({
        1: 'a',
        2: 'b'
    }, 'd', expected_error=KeyError)


def test_set_item():
    def set_item(my_dict, key, value):
        my_dict[key] = value
        return my_dict

    set_item_verifier = verifier_for(set_item)

    set_item_verifier.verify(dict(), 'a', 1, expected_result={
        'a': 1
    })
    set_item_verifier.verify({'a': 1}, 'a', 2, expected_result={
        'a': 2
    })
    set_item_verifier.verify({'a': 1, 'b': 2}, 'c', 3, expected_result={
        'a': 1,
        'b': 2,
        'c': 3
    })
    set_item_verifier.verify({'a': 1, 'b': 2}, 'c', 2, expected_result={
        'a': 1,
        'b': 2,
        'c': 2
    })


def test_delete_item():
    def delete_item(my_dict, key):
        del my_dict[key]
        return my_dict

    delete_item_verifier = verifier_for(delete_item)

    delete_item_verifier.verify({1: 'a', 2: 'b'}, 1, expected_result={2: 'b'})
    delete_item_verifier.verify({1: 'a', 2: 'b'}, 2, expected_result={1: 'a'})
    delete_item_verifier.verify({1: 'a'}, 1, expected_result=dict())
    delete_item_verifier.verify({1: 'a', 2: 'b'}, 'a', expected_error=KeyError)
    delete_item_verifier.verify({1: 'a', 2: 'b'}, 'd', expected_error=KeyError)
    delete_item_verifier.verify(dict(), 'a', expected_error=KeyError)


def test_clear():
    def clear(my_dict):
        my_dict.clear()
        return my_dict

    clear_verifier = verifier_for(clear)

    clear_verifier.verify({1: 'a', 2: 'b'}, expected_result=dict())
    clear_verifier.verify({2: 'b'}, expected_result=dict())
    clear_verifier.verify(dict(), expected_result=dict())


def test_copy():
    def copy(my_dict):
        out = my_dict.copy()
        return out, my_dict is out

    copy_verifier = verifier_for(copy)

    copy_verifier.verify({1: 'a', 2: 'b'}, expected_result=({1: 'a', 2: 'b'}, False))
    copy_verifier.verify({2: 'b'}, expected_result=({2: 'b'}, False))
    copy_verifier.verify(dict(), expected_result=(dict(), False))


def test_get():
    get_verifier = verifier_for(lambda my_dict, key: my_dict.get(key))
    get_with_default_verifier = verifier_for(lambda my_dict, key, default: my_dict.get(key, default))

    get_verifier.verify({
        1: 'a',
        2: 'b'
    }, 1, expected_result='a')
    get_verifier.verify({
        1: 'a',
        2: 'b'
    }, 2, expected_result='b')
    get_verifier.verify({
        1: 'a',
        2: 'b'
    }, 'a', expected_result=None)
    get_verifier.verify({
        1: 'a',
        2: 'b'
    }, 'd', expected_result=None)

    get_with_default_verifier.verify({
        1: 'a',
        2: 'b'
    }, 1, 10, expected_result='a')
    get_with_default_verifier.verify({
        1: 'a',
        2: 'b'
    }, 2, 20, expected_result='b')
    get_with_default_verifier.verify({
        1: 'a',
        2: 'b'
    }, 'a', 'A', expected_result='A')
    get_with_default_verifier.verify({
        1: 'a',
        2: 'b'
    }, 'd', 'D', expected_result='D')


def test_items():
    def items_with_modification(my_dict):
        out = my_dict.items()
        my_dict['extra'] = 10
        return list(out)

    items_verifier = verifier_for(lambda my_dict: list(my_dict.items()))
    items_with_modification_verifier = verifier_for(items_with_modification)

    items_verifier.verify(dict(), expected_result=[])
    items_verifier.verify({'key': 'value'}, expected_result=[('key', 'value')])
    items_verifier.verify({
        1: 'a',
        2: 'b'
    }, expected_result=[(1, 'a'), (2, 'b')])
    items_verifier.verify({
        'a': 1,
        'b': 2
    }, expected_result=[('a', 1), ('b', 2)])

    items_with_modification_verifier.verify(dict(), expected_result=[('extra', 10)])
    items_with_modification_verifier.verify({'key': 'value'}, expected_result=[('key', 'value'), ('extra', 10)])
    items_with_modification_verifier.verify({
        1: 'a',
        2: 'b'
    }, expected_result=[(1, 'a'), (2, 'b'), ('extra', 10)])
    items_with_modification_verifier.verify({
        'a': 1,
        'b': 2
    }, expected_result=[('a', 1), ('b', 2), ('extra', 10)])
    items_with_modification_verifier.verify({
        'extra': 1,
        'b': 2
    }, expected_result=[('extra', 10), ('b', 2)])


def test_keys():
    def keys_with_modification(my_dict):
        out = my_dict.keys()
        my_dict['extra'] = 10
        return list(out)

    keys_verifier = verifier_for(lambda my_dict: list(my_dict.keys()))
    keys_with_modification_verifier = verifier_for(keys_with_modification)

    keys_verifier.verify(dict(), expected_result=[])
    keys_verifier.verify({'key': 'value'}, expected_result=['key'])
    keys_verifier.verify({
        1: 'a',
        2: 'b'
    }, expected_result=[1, 2])
    keys_verifier.verify({
        'a': 1,
        'b': 2
    }, expected_result=['a', 'b'])

    keys_with_modification_verifier.verify(dict(), expected_result=['extra'])
    keys_with_modification_verifier.verify({'key': 'value'}, expected_result=['key', 'extra'])
    keys_with_modification_verifier.verify({
        1: 'a',
        2: 'b'
    }, expected_result=[1, 2, 'extra'])
    keys_with_modification_verifier.verify({
        'a': 1,
        'b': 2
    }, expected_result=['a', 'b', 'extra'])
    keys_with_modification_verifier.verify({
        'extra': 1,
        'b': 2
    }, expected_result=['extra', 'b'])


def test_values():
    def values_with_modification(my_dict):
        out = my_dict.values()
        my_dict['extra'] = 10
        return list(out)

    values_verifier = verifier_for(lambda my_dict: list(my_dict.values()))
    values_with_modification_verifier = verifier_for(values_with_modification)

    values_verifier.verify(dict(), expected_result=[])
    values_verifier.verify({'key': 'value'}, expected_result=['value'])
    values_verifier.verify({
        1: 'a',
        2: 'b'
    }, expected_result=['a', 'b'])
    values_verifier.verify({
        'a': 1,
        'b': 2
    }, expected_result=[1, 2])
    values_verifier.verify({
        'a': 1,
        'b': 2,
        'c': 2
    }, expected_result=[1, 2, 2])

    values_with_modification_verifier.verify(dict(), expected_result=[10])
    values_with_modification_verifier.verify({'key': 'value'}, expected_result=['value', 10])
    values_with_modification_verifier.verify({
        1: 'a',
        2: 'b'
    }, expected_result=['a', 'b', 10])
    values_with_modification_verifier.verify({
        'a': 1,
        'b': 2
    }, expected_result=[1, 2, 10])
    values_with_modification_verifier.verify({
        'a': 1,
        'b': 2,
        'c': 2,
    }, expected_result=[1, 2, 2, 10])
    values_with_modification_verifier.verify({
        'extra': 1,
        'b': 2
    }, expected_result=[10, 2])


def test_pop():
    def pop(my_dict, key):
        out = my_dict.pop(key)
        return out, my_dict

    def pop_with_default(my_dict, key, default):
        out = my_dict.pop(key, default)
        return out, my_dict

    pop_verifier = verifier_for(pop)
    pop_with_default_verifier = verifier_for(pop_with_default)

    pop_verifier.verify({
        'a': 1,
        'b': 2
    }, 'a', expected_result=(1, {'b': 2}))
    pop_verifier.verify({
        'a': 1,
        'b': 2
    }, 'b', expected_result=(2, {'a': 1}))
    pop_verifier.verify({
        'a': 1,
        'b': 2
    }, 'c', expected_error=KeyError)

    pop_with_default_verifier.verify({
        'a': 1,
        'b': 2
    }, 'a', 3, expected_result=(1, {'b': 2}))
    pop_with_default_verifier.verify({
        'a': 1,
        'b': 2
    }, 'b', 3, expected_result=(2, {'a': 1}))
    pop_with_default_verifier.verify({
        'a': 1,
        'b': 2
    }, 'c', 3, expected_result=(3, {'a': 1, 'b': 2}))


def test_popitem():
    def popitem(my_dict):
        out = my_dict.popitem()
        return out, my_dict

    popitem_verifier = verifier_for(popitem)

    popitem_verifier.verify({
        1: 'a',
        2: 'b'
    }, expected_result=((2, 'b'), {1: 'a'}))
    popitem_verifier.verify({
        'b': 2,
        'a': 1
    }, expected_result=(('a', 1), {'b': 2}))
    popitem_verifier.verify({
        'b': 2,
        'a': 1,
        'c': 3
    }, expected_result=(('c', 3), {'b': 2, 'a': 1}))
    popitem_verifier.verify(dict(), expected_error=KeyError)


def test_reversed():
    def to_reversed_list(x):
        return list(reversed(x))

    to_reversed_list_verifier = verifier_for(to_reversed_list)

    to_reversed_list_verifier.verify(dict(), expected_result=[])
    to_reversed_list_verifier.verify({
        1: 'a',
        2: 'b'
    }, expected_result=[2, 1])
    to_reversed_list_verifier.verify({
        'a': 1,
        'b': 2
    }, expected_result=['b', 'a'])
    to_reversed_list_verifier.verify({
        3: 'a',
        2: 'b',
        1: 'c'
    }, expected_result=[1, 2, 3])


def test_setdefault():
    def put(my_dict, key):
        out = my_dict.setdefault(key)
        return out, my_dict

    def put_with_default(my_dict, key, default):
        out = my_dict.setdefault(key, default)
        return out, my_dict

    put_verifier = verifier_for(put)
    put_with_default_verifier = verifier_for(put_with_default)

    put_verifier.verify({
        1: 'a',
        2: 'b'
    }, 1, expected_result=('a', {1: 'a', 2: 'b'}))
    put_verifier.verify({
        1: 'a',
        2: 'b'
    }, 2, expected_result=('b', {1: 'a', 2: 'b'}))
    put_verifier.verify({
        1: 'a',
        2: 'b'
    }, 'a', expected_result=(None, {1: 'a', 2: 'b', 'a': None}))
    put_verifier.verify({
        1: 'a',
        2: 'b'
    }, 'd', expected_result=(None, {1: 'a', 2: 'b', 'd': None}))

    put_with_default_verifier.verify({
        1: 'a',
        2: 'b'
    }, 1, 10, expected_result=('a', {1: 'a', 2: 'b'}))
    put_with_default_verifier.verify({
        1: 'a',
        2: 'b'
    }, 2, 10, expected_result=('b', {1: 'a', 2: 'b'}))
    put_with_default_verifier.verify({
        1: 'a',
        2: 'b'
    }, 'a', 10, expected_result=(10, {1: 'a', 2: 'b', 'a': 10}))
    put_with_default_verifier.verify({
        1: 'a',
        2: 'b'
    }, 'd', 100, expected_result=(100, {1: 'a', 2: 'b', 'd': 100}))


def test_update():
    def update(my_dict, items):
        my_dict.update(items)
        return my_dict

    update_verifier = verifier_for(update)

    update_verifier.verify({
        'a': 1,
        'b': 2
    }, {
        'c': 3,
        'd': 4
    }, expected_result={'a': 1, 'b': 2, 'c': 3, 'd': 4})
    update_verifier.verify({
        'a': 1,
        'b': 2
    }, {
        'b': 3,
        'd': 4
    }, expected_result={'a': 1, 'b': 3, 'd': 4})
    update_verifier.verify({
        'a': 1,
        'b': 2
    }, [
        ('c', 3),
        ('d', 4)
    ], expected_result={'a': 1, 'b': 2, 'c': 3, 'd': 4})
    update_verifier.verify({
        'a': 1,
        'b': 2
    }, [
        ('b', 3),
        ('d', 4)
    ], expected_result={'a': 1, 'b': 3, 'd': 4})
