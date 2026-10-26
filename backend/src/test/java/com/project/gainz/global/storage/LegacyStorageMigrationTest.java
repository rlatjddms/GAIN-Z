package com.project.gainz.global.storage;

import org.junit.jupiter.api.Test;
import org.springframework.core.io.ByteArrayResource;

import java.io.IOException;
import java.io.InputStream;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

// Supabase → R2 한 번 옮기기 (#262)
class LegacyStorageMigrationTest {

    private final R2FileStore r2 = mock(R2FileStore.class);
    private final SupabaseFileStore legacy = mock(SupabaseFileStore.class);
    private final LegacyStorageMigration migration = new LegacyStorageMigration(r2);

    @Test
    void R2에_없는_파일만_옮기고_실패한_파일은_넘어간다() throws IOException {
        when(r2.legacy()).thenReturn(legacy);
        when(legacy.list("sheets")).thenReturn(List.of("sheets/a.png", "sheets/b.png"));
        when(legacy.list("profiles")).thenThrow(new IOException("목록 실패"));
        when(legacy.list("band")).thenReturn(List.of("band/c", "band/d"));
        when(r2.inR2("sheets/a.png")).thenReturn(true);
        when(legacy.get("sheets/b.png")).thenReturn(new ByteArrayResource(new byte[] {1, 2}));
        when(legacy.get("band/c")).thenReturn(new ByteArrayResource(new byte[] {3}));
        when(legacy.get("band/d")).thenThrow(new IOException("받기 실패"));

        assertThat(migration.migrate()).isEqualTo(2);

        verify(r2, never()).put(eq("sheets/a.png"), any(InputStream.class), anyLong(), anyString());
        verify(r2).put(eq("sheets/b.png"), any(InputStream.class), eq(2L), anyString());
        verify(r2).put(eq("band/c"), any(InputStream.class), eq(1L), anyString());
        verify(r2, never()).put(eq("band/d"), any(InputStream.class), anyLong(), anyString());
    }

    @Test
    void Supabase_정보가_없으면_아무것도_안_한다() throws IOException {
        migration.run(null);
        verify(r2, never()).put(anyString(), any(InputStream.class), anyLong(), anyString());
    }
}
