package com.example.xyzreader.ui;

import android.annotation.TargetApi;
import android.app.Fragment;
import android.app.LoaderManager;
import android.content.Intent;
import android.content.Loader;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.graphics.Color;
import android.graphics.Rect;
import android.graphics.Typeface;
import android.graphics.drawable.ColorDrawable;
import android.graphics.drawable.Drawable;
import android.graphics.drawable.GradientDrawable;
import android.os.Build;
import android.os.Bundle;
import android.support.design.widget.AppBarLayout;
import android.support.design.widget.CollapsingToolbarLayout;
import android.support.v4.app.ShareCompat;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.graphics.Palette;
import android.support.v7.widget.Toolbar;
import android.text.Html;
import android.text.format.DateUtils;
import android.text.method.LinkMovementMethod;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.WindowManager;
import android.view.animation.AlphaAnimation;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import com.example.xyzreader.R;
import com.example.xyzreader.data.ArticleLoader;
import com.squareup.picasso.Picasso;
import com.squareup.picasso.Target;

/**
 * A fragment representing a single Article detail screen. This fragment is
 * either contained in a {@link ArticleListActivity} in two-pane mode (on
 * tablets) or a {@link ArticleDetailActivity} on handsets.
 */
public class ArticleDetailFragment extends Fragment implements
        LoaderManager.LoaderCallbacks<Cursor>, AppBarLayout.OnOffsetChangedListener {

    private static final String TAG = "ArticleDetailFragment";

    public static final String ARG_ITEM_ID = "ARG_ITEM_ID";

    private static final float PERCENTAGE_TO_HIDE_TITLE_DETAILS = 0.4f;
    private static final int ALPHA_ANIMATIONS_DURATION = 200;
    private static final String IS_TITLE_VISIBLE_KEY = "IS_TITLE_VISIBLE_KEY";

    private boolean mIsTheTitleVisible = false;

    private Cursor mCursor;
    private long mItemId;
    private View mRootView;
    private ImageView mPhotoView;
    private Target mBitmapTarget;
    private LinearLayout mTitleContainer;
    private String mTitle;
    private TextView mTitleView;
    private TextView mBylineView;
    private TextView mTvBody;
    private int mMutedColor;
    private AppBarLayout mAppBarLayout;
    private Toolbar mToolbar;
    private CollapsingToolbarLayout mCollapsingToolbarLayout;
    private View mTitleGradient;
    private boolean isCurrentFragment = false;

    /**
     * Mandatory empty constructor for the fragment manager to instantiate the
     * fragment (e.g. upon screen orientation changes).
     */
    public ArticleDetailFragment() {
    }

    public static ArticleDetailFragment newInstance(long itemId) {
        Bundle arguments = new Bundle();
        arguments.putLong(ARG_ITEM_ID, itemId);
        ArticleDetailFragment fragment = new ArticleDetailFragment();
        fragment.setArguments(arguments);
        return fragment;
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        if (savedInstanceState != null) {
            mIsTheTitleVisible = savedInstanceState.getBoolean(IS_TITLE_VISIBLE_KEY);
        }

        if (getArguments().containsKey(ARG_ITEM_ID)) {
            mItemId = getArguments().getLong(ARG_ITEM_ID);
        }

        //setHasOptionsMenu(true);
    }

    public ArticleDetailActivity getActivityCast() {
        return (ArticleDetailActivity) getActivity();
    }

    @Override
    public void onActivityCreated(Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);

        // In support library r8, calling initLoader for a fragment in a FragmentPagerAdapter in
        // the fragment's onCreate may cause the same LoaderManager to be dealt to multiple
        // fragments because their mIndex is -1 (haven't been added to the activity yet). Thus,
        // we do this in onActivityCreated.
        getLoaderManager().initLoader(0, null, this);
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        mRootView = inflater.inflate(R.layout.fragment_article_detail, container, false);

        mRootView.findViewById(R.id.share_fab).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                startActivity(Intent.createChooser(ShareCompat.IntentBuilder.from(getActivity())
                        .setType("text/plain")
                        .setText("Some sample text")
                        .getIntent(), getString(R.string.action_share)));
            }
        });

        bindViews();

        return mRootView;
    }

    private void bindViews() {
        if (mRootView == null) {
            return;
        }

        mTitleContainer = (LinearLayout) mRootView.findViewById(R.id.ll_title_container);
        mTitleContainer.setVisibility(View.INVISIBLE);
        mTitleView = (TextView) mRootView.findViewById(R.id.article_title);
        mBylineView = (TextView) mRootView.findViewById(R.id.article_byline);
        mTvBody = (TextView) mRootView.findViewById(R.id.article_body);
        mTvBody.setTypeface(Typeface.createFromAsset(getResources().getAssets(), "Rosario-Regular.ttf"));

        mPhotoView = (ImageView) mRootView.findViewById(R.id.ivArticleImage);
        mAppBarLayout = (AppBarLayout) mRootView.findViewById(R.id.app_bar_layout);
        mToolbar = (Toolbar) mRootView.findViewById(R.id.toolbar);
        mCollapsingToolbarLayout = (CollapsingToolbarLayout) mRootView.findViewById(R.id.collapsingToolbarLayout);
        mTitleGradient = mRootView.findViewById(R.id.vTitleGradient);

        //if (mIsTheTitleVisible)
            mToolbar.setTitle("");

        mAppBarLayout.addOnOffsetChangedListener(this);

        if (mCursor != null) {

            mTitle = mCursor.getString(ArticleLoader.Query.TITLE);

            if (!mIsTheTitleVisible)
                mToolbar.setTitle(mTitle);

            ((AppCompatActivity) getActivity()).setSupportActionBar(mToolbar);
            ((AppCompatActivity) getActivity()).getSupportActionBar().setDisplayHomeAsUpEnabled(true);

            mTitleView.setText(mTitle);

            if (mIsTheTitleVisible)
                mTitleContainer.setVisibility(View.VISIBLE);

            mBylineView.setText(Html.fromHtml(DateUtils.getRelativeTimeSpanString(
                    mCursor.getLong(ArticleLoader.Query.PUBLISHED_DATE),
                    System.currentTimeMillis(), DateUtils.HOUR_IN_MILLIS,
                    DateUtils.FORMAT_ABBREV_ALL).toString()
                    + " by <font color='#ffffff'>"
                    + mCursor.getString(ArticleLoader.Query.AUTHOR)
                    + "</font>"));
            mTvBody.setText(Html.fromHtml(mCursor.getString(ArticleLoader.Query.BODY)));

            final String imageURL = mCursor.getString(ArticleLoader.Query.PHOTO_URL);

            mBitmapTarget = new Target() {
                @Override
                public void onBitmapLoaded(Bitmap bitmap, Picasso.LoadedFrom from) {
                    Log.d(TAG, "Bitmap loaded from " + imageURL);
                    if (bitmap != null) {
                        Palette.Builder paletteBuilder = new Palette.Builder(bitmap);
                        Palette p = paletteBuilder.generate();

                        mMutedColor = p.getDarkMutedColor(0xAAAAAAAA); // TODO: get default color from resources

                        int[] colors = new int[] {Color.parseColor("#00000000"), mMutedColor};
                        GradientDrawable gd = new GradientDrawable(GradientDrawable.Orientation.TOP_BOTTOM, colors);
                        mTitleGradient.setBackground(gd);

                        mCollapsingToolbarLayout.setContentScrimColor(mMutedColor);

                        if (isCurrentFragment && mMutedColor != 0 && Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP)
                            getActivity().getWindow().setStatusBarColor(mMutedColor);

                        mPhotoView.setImageBitmap(bitmap);
                    }
                }

                @Override
                public void onBitmapFailed(Drawable errorDrawable) {
                    Log.e(TAG, "Error loading image from " + imageURL);
                }

                @Override
                public void onPrepareLoad(Drawable placeHolderDrawable) {
                }
            };

            Picasso p = Picasso.with(getActivity());
            p.setIndicatorsEnabled(true);
            p.setLoggingEnabled(true);
            p.load(imageURL)
                    .into(mBitmapTarget);

        } else {
            // TODO show empty message
            //mRootView.setVisibility(View.GONE);
            //mCollapsingToolbarLayout.setTitle("N/A");
            //mTvBody.setText("N/A");
        }
    }

    @Override
    public void setMenuVisibility(boolean menuVisible) {
        super.setMenuVisibility(menuVisible);

        isCurrentFragment = menuVisible;

        if (menuVisible && mMutedColor != 0 && Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP)
            getActivity().getWindow().setStatusBarColor(mMutedColor);
    }

    @Override
    public Loader<Cursor> onCreateLoader(int i, Bundle bundle) {
        return ArticleLoader.newInstanceForItemId(getActivity(), mItemId);
    }

    @Override
    public void onLoadFinished(Loader<Cursor> cursorLoader, Cursor cursor) {
        if (!isAdded()) {
            if (cursor != null) {
                cursor.close();
            }
            return;
        }

        mCursor = cursor;
        if (mCursor != null && !mCursor.moveToFirst()) {
            Log.e(TAG, "Error reading item detail cursor");

            mCursor.close();
            mCursor = null;
        }

        bindViews();
    }

    @Override
    public void onLoaderReset(Loader<Cursor> cursorLoader) {
        mCursor = null;
        bindViews();
    }

    @Override
    public void onOffsetChanged(AppBarLayout appBarLayout, int offset) {
        int maxScroll = appBarLayout.getTotalScrollRange();
        float percentage = (float) Math.abs(offset) / (float) maxScroll;

        if (percentage >= PERCENTAGE_TO_HIDE_TITLE_DETAILS) {
            if(mIsTheTitleVisible) {
                mToolbar.setTitle(mTitle);
                startAlphaAnimation(mTitleContainer, ALPHA_ANIMATIONS_DURATION, View.INVISIBLE);
                mIsTheTitleVisible = false;
            }
        } else {
            if(!mIsTheTitleVisible) {
                mToolbar.setTitle("");
                startAlphaAnimation(mTitleContainer, ALPHA_ANIMATIONS_DURATION, View.VISIBLE);
                mIsTheTitleVisible = true;
            }
        }
    }

    @Override
    public void onSaveInstanceState(Bundle outState) {
        outState.putBoolean(IS_TITLE_VISIBLE_KEY, mIsTheTitleVisible);
        super.onSaveInstanceState(outState);
    }

    private void startAlphaAnimation(View v, long duration, int visibility) {
        AlphaAnimation alphaAnimation = (visibility == View.VISIBLE)
                ? new AlphaAnimation(0f, 1f)
                : new AlphaAnimation(1f, 0f);

        alphaAnimation.setDuration(duration);
        alphaAnimation.setFillAfter(true);
        v.startAnimation(alphaAnimation);
    }
}
