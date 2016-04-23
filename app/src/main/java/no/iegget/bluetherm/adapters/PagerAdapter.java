package no.iegget.bluetherm.adapters;

import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentStatePagerAdapter;

import no.iegget.bluetherm.ChartFragment;
import no.iegget.bluetherm.GeneralFragment;

/**
 * Created by iver on 23/04/16.
 */
public class PagerAdapter extends FragmentStatePagerAdapter {

    int mNumOfTabs;

    public PagerAdapter(FragmentManager fm, int NumOfTabs) {
        super(fm);
        this.mNumOfTabs = NumOfTabs;
    }

    @Override
    public Fragment getItem(int position) {

        switch (position) {
            case 0:
                GeneralFragment generalFragment = new GeneralFragment();
                return generalFragment;
            case 1:
                ChartFragment chartFragment = new ChartFragment();
                return chartFragment;
            default:
                return null;
        }
    }

    @Override
    public int getCount() {
        return mNumOfTabs;
    }
}
