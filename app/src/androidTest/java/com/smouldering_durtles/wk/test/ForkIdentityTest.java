package com.smouldering_durtles.wk.test;

import android.app.SearchManager;
import android.app.SearchableInfo;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.content.pm.ProviderInfo;
import android.net.Uri;

import androidx.test.core.app.ApplicationProvider;
import androidx.test.ext.junit.runners.AndroidJUnit4;

import com.smouldering_durtles.wk.BuildConfig;
import com.smouldering_durtles.wk.Identification;
import com.smouldering_durtles.wk.R;
import com.smouldering_durtles.wk.activities.AboutActivity;
import com.smouldering_durtles.wk.activities.BrowseActivity;

import org.junit.Test;
import org.junit.runner.RunWith;

import static org.junit.Assert.*;

@RunWith(AndroidJUnit4.class)
public class ForkIdentityTest {
    @Test
    public void providersAndSubjectLinksBelongToFork() {
        Context context = ApplicationProvider.getApplicationContext();
        PackageManager pm = context.getPackageManager();
        assertNotEquals("com.smouldering_durtles.wk.debug", context.getPackageName());
        assertNotEquals("com.smouldering_durtles.wk", context.getPackageName());
        for (int resource : new int[]{R.string.fileprovider_authority, R.string.searchprovider_authority}) {
            String authority = context.getString(resource);
            assertTrue(authority.startsWith(context.getPackageName() + "."));
            ProviderInfo provider = pm.resolveContentProvider(authority, 0);
            assertNotNull(provider);
            assertEquals(context.getPackageName(), provider.packageName);
            assertFalse(provider.exported);
        }
        assertEquals(context.getString(R.string.fileprovider_authority), BuildConfig.FILEPROVIDER_AUTHORITY);
        assertEquals(Identification.APP_URI_SCHEME, context.getString(R.string.uri_scheme));
        Intent link = new Intent(Intent.ACTION_VIEW, Uri.parse(context.getString(R.string.subject_info_uri) + "/1"))
                .setPackage(context.getPackageName());
        assertEquals(new ComponentName(context, BrowseActivity.class), link.resolveActivity(pm));
        link.setData(Uri.parse("smouldering-durtles://subject-info/1"));
        assertNull("The fork must not claim upstream subject links", link.resolveActivity(pm));
    }

    @Test
    public void defaultSearchableResolvesDespiteDifferentJavaNamespace() {
        Context context = ApplicationProvider.getApplicationContext();
        SearchManager manager = (SearchManager) context.getSystemService(Context.SEARCH_SERVICE);
        SearchableInfo searchable = manager.getSearchableInfo(new ComponentName(context, AboutActivity.class));
        assertNotNull("Default searchable metadata must use the actual class name", searchable);
        assertEquals(new ComponentName(context, BrowseActivity.class), searchable.getSearchActivity());
        assertEquals(context.getString(R.string.searchprovider_authority), searchable.getSuggestAuthority());
    }
}
