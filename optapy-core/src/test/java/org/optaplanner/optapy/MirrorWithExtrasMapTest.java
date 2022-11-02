package org.optaplanner.optapy;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;

import java.util.Map;

import org.junit.jupiter.api.Test;

public class MirrorWithExtrasMapTest {

    @Test
    public void testGet() {
        Map<String, String> baseMap = Map.of("a", "1", "b", "2");
        MirrorWithExtrasMap<String, String> mirrorMap = new MirrorWithExtrasMap<>(baseMap);

        assertThat(mirrorMap.get("a")).isEqualTo("1");
        assertThat(mirrorMap.get("b")).isEqualTo("2");
        assertThat(mirrorMap.get("c")).isNull();
    }

    @Test
    public void testPut() {
        Map<String, String> baseMap = Map.of("a", "1", "b", "2");
        MirrorWithExtrasMap<String, String> mirrorMap = new MirrorWithExtrasMap<>(baseMap);

        mirrorMap.put("c", "3");

        assertThat(baseMap.containsKey("c")).isFalse();
        assertThat(mirrorMap.get("a")).isEqualTo("1");
        assertThat(mirrorMap.get("b")).isEqualTo("2");
        assertThat(mirrorMap.get("c")).isEqualTo("3");
    }

    @Test
    public void testInvalidPut() {
        Map<String, String> baseMap = Map.of("a", "1", "b", "2");
        MirrorWithExtrasMap<String, String> mirrorMap = new MirrorWithExtrasMap<>(baseMap);

        assertThatCode(() -> mirrorMap.put("a", "3")).isInstanceOf(IllegalArgumentException.class);
        assertThat(baseMap).isEqualTo(Map.of("a", "1", "b", "2"));
        assertThat(mirrorMap).isEqualTo(Map.of("a", "1", "b", "2"));
    }

    @Test
    public void testPutAll() {
        Map<String, String> baseMap = Map.of("a", "1", "b", "2");
        MirrorWithExtrasMap<String, String> mirrorMap = new MirrorWithExtrasMap<>(baseMap);

        mirrorMap.putAll(Map.of("c", "3", "d", "4"));

        assertThat(baseMap.containsKey("c")).isFalse();
        assertThat(baseMap.containsKey("d")).isFalse();

        assertThat(mirrorMap.get("a")).isEqualTo("1");
        assertThat(mirrorMap.get("b")).isEqualTo("2");
        assertThat(mirrorMap.get("c")).isEqualTo("3");
        assertThat(mirrorMap.get("d")).isEqualTo("4");
    }

    @Test
    public void testInvalidPutAll() {
        Map<String, String> baseMap = Map.of("a", "1", "b", "2");
        MirrorWithExtrasMap<String, String> mirrorMap = new MirrorWithExtrasMap<>(baseMap);

        assertThatCode(() -> mirrorMap.putAll(Map.of("c", "3", "a", "3"))).isInstanceOf(IllegalArgumentException.class);
        assertThat(baseMap).isEqualTo(Map.of("a", "1", "b", "2"));
        assertThat(mirrorMap).isEqualTo(Map.of("a", "1", "b", "2"));
    }

    @Test
    public void testContainsKey() {
        Map<String, String> baseMap = Map.of("a", "1", "b", "2");
        MirrorWithExtrasMap<String, String> mirrorMap = new MirrorWithExtrasMap<>(baseMap);

        assertThat(mirrorMap.containsKey("a")).isTrue();
        assertThat(mirrorMap.containsKey("1")).isFalse();
        assertThat(mirrorMap.containsKey("c")).isFalse();
        mirrorMap.put("c", "3");
        assertThat(mirrorMap.containsKey("c")).isTrue();
    }

    @Test
    public void testContainsValue() {
        Map<String, String> baseMap = Map.of("a", "1", "b", "2");
        MirrorWithExtrasMap<String, String> mirrorMap = new MirrorWithExtrasMap<>(baseMap);

        assertThat(mirrorMap.containsValue("1")).isTrue();
        assertThat(mirrorMap.containsValue("a")).isFalse();
        assertThat(mirrorMap.containsValue("3")).isFalse();
        mirrorMap.put("c", "3");
        assertThat(mirrorMap.containsValue("3")).isTrue();
    }

    @Test
    public void testSize() {
        Map<String, String> baseMap = Map.of("a", "1", "b", "2");
        MirrorWithExtrasMap<String, String> mirrorMap = new MirrorWithExtrasMap<>(baseMap);

        assertThat(mirrorMap.size()).isEqualTo(2);
        mirrorMap.put("c", "3");
        assertThat(mirrorMap.size()).isEqualTo(3);
    }

    @Test
    public void testIsEmpty() {
        Map<String, String> baseMap = Map.of("a", "1", "b", "2");
        MirrorWithExtrasMap<String, String> mirrorMap = new MirrorWithExtrasMap<>(baseMap);

        assertThat(mirrorMap.isEmpty()).isFalse();
        mirrorMap.put("c", "3");
        assertThat(mirrorMap.isEmpty()).isFalse();

        baseMap = Map.of();
        mirrorMap = new MirrorWithExtrasMap<>(baseMap);

        assertThat(mirrorMap.isEmpty()).isTrue();
        mirrorMap.put("a", "1");
        assertThat(mirrorMap.isEmpty()).isFalse();
    }

    @Test
    public void testEntrySet() {
        Map<String, String> baseMap = Map.of("a", "1", "b", "2");
        MirrorWithExtrasMap<String, String> mirrorMap = new MirrorWithExtrasMap<>(baseMap);

        assertThat(mirrorMap.entrySet()).containsExactlyInAnyOrder(Map.entry("a", "1"),
                Map.entry("b", "2"));
        mirrorMap.put("c", "3");
        assertThat(mirrorMap.entrySet()).containsExactlyInAnyOrder(Map.entry("a", "1"),
                Map.entry("b", "2"),
                Map.entry("c", "3"));
    }

}
