package com.dodola.flip;

import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentTransaction;
import android.support.v7.app.ActionBarActivity;
import android.os.Bundle;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;

import com.dodola.flip.dummy.SimpleData;


public class MainActivity extends ActionBarActivity implements RecyclerFragment.OnFragmentInteractionListener {
    FoldLayout foldLayout;

    @Override
    public void onBackPressed() {
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        foldLayout = (FoldLayout) this.findViewById(R.id.main_view_container);
        foldLayout.setFragmentManager(this.getSupportFragmentManager());
        RecyclerFragment feedFragment = RecyclerFragment.newInstance();
        final FragmentTransaction fragmentTransaction = getSupportFragmentManager().beginTransaction();
        Fragment detailFrag = getSupportFragmentManager().findFragmentByTag(FoldLayout.FRAGMENT_DETAIL_VIEW_TAG
        );
        fragmentTransaction.replace(R.id.main_view_container, feedFragment, "feed");
        if (detailFrag != null) {
            fragmentTransaction.remove(detailFrag);
        }
        fragmentTransaction.commit();
    }

    void openItemDetailView(String url, int location) {
        DetailFragment detail = DetailFragment.newInstance(url);
        this.foldLayout.setFoldCenter(location);
        getSupportFragmentManager().beginTransaction().add(R.id.main_view_container, detail, FoldLayout.FRAGMENT_DETAIL_VIEW_TAG)
                .commitAllowingStateLoss();
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.menu_main, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        return super.onOptionsItemSelected(item);
    }

    @Override
    public void onFragmentInteraction(SimpleData data, View view) {
        int[] containerLocation = new int[2];
        this.foldLayout.getLocationInWindow(containerLocation);
        int[] viewLocation = new int[2];
        view.getLocationInWindow(viewLocation);
        int location = viewLocation[1] + getResources().getDimensionPixelSize(R.dimen.image_height) - containerLocation[1];
        openItemDetailView(data.url, location);
    }
}
