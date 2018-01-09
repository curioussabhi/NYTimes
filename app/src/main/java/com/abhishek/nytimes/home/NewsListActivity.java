package com.abhishek.nytimes.home;

import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.support.design.widget.Snackbar;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.support.v7.widget.SearchView;
import android.support.v7.widget.Toolbar;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.TextView;

import com.abhishek.nytimes.R;
import com.abhishek.nytimes.app.NYTApplication;
import com.abhishek.nytimes.details.DetailsActivity;
import com.abhishek.nytimes.model.Credit;
import com.abhishek.nytimes.model.NewsItem;
import com.squareup.picasso.Picasso;

import javax.inject.Inject;

public class NewsListActivity extends AppCompatActivity implements INewsListView, MenuItem.OnActionExpandListener, SearchView.OnQueryTextListener {

    private static final String SEARCH_TYPE_KEY = "searchType";
    private static final int loadNextThreshold = 4;

    private ProgressBar progressBar;
    private Snackbar snackbar;

    private NewsAdapter adapter;
    private SearchType currentSearchType;
    private Snackbar.Callback snackbarCallback;

    @Inject
    INewsListPresenter presenter;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_home);

        progressBar = findViewById(R.id.progressBar);
        Toolbar toolbar = findViewById(R.id.homeToolbar);
        setSupportActionBar(toolbar);
        RecyclerView rcView = findViewById(R.id.rcView);
        LinearLayoutManager layoutManager = new LinearLayoutManager(this);
        rcView.setLayoutManager(layoutManager);
        rcView.addOnScrollListener(new RecyclerView.OnScrollListener() {
            @Override
            public void onScrolled(RecyclerView recyclerView, int dx, int dy) {
                if (!presenter.isOnGoing(currentSearchType)) {
                    if (dy > 0) {
                        int lastVisibleItem = layoutManager.findLastVisibleItemPosition();
                        int totalItems = layoutManager.getItemCount();

                        if (lastVisibleItem + loadNextThreshold >= totalItems)
                            presenter.getNextPage(currentSearchType);
                    }
                }
            }
        });
        adapter = new NewsAdapter();
        rcView.setAdapter(adapter);
        snackbarCallback = new Snackbar.Callback() {
            @Override
            public void onDismissed(Snackbar transientBottomBar, int event) {
                super.onDismissed(transientBottomBar, event);
                snackbar = null;
            }
        };

        if (savedInstanceState != null)
            currentSearchType = (SearchType) savedInstanceState.getSerializable(SEARCH_TYPE_KEY);
        else
            currentSearchType = SearchType.Recent;

        NYTApplication.getComponent().injectActivity(this);
        presenter.onAttachView(this, currentSearchType);

        tryInitializeRecent();
    }

    @Override
    protected void onDestroy() {
        presenter.onDetachView();
        super.onDestroy();
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.menu_main, menu);

        MenuItem searchItem = menu.findItem(R.id.searchView);
        searchItem.setOnActionExpandListener(this);

        SearchView searchView = (SearchView) searchItem.getActionView();
        searchView.setIconifiedByDefault(true);
        searchView.setOnQueryTextListener(this);

        if (currentSearchType == SearchType.Custom) {
            searchView.setIconified(false);
            searchItem.expandActionView();
        }

        return true;
    }

    @Override
    public boolean onMenuItemActionExpand(MenuItem item) {
        currentSearchType = SearchType.Custom;
        adapter.notifyDataSetChanged();

        setOngoing(presenter.isOnGoing(currentSearchType), currentSearchType);
        return true;
    }

    @Override
    public boolean onMenuItemActionCollapse(MenuItem item) {
        currentSearchType = SearchType.Recent;
        adapter.notifyDataSetChanged();
        setOngoing(presenter.isOnGoing(currentSearchType), currentSearchType);

        // In case recent news wasn't loaded dude to some error
        tryInitializeRecent();

        presenter.setQuery(null);
        presenter.clearNews(SearchType.Custom);
        return true;
    }

    @Override
    public boolean onQueryTextSubmit(String query) {
        presenter.setQuery(query);
        return true;
    }

    @Override
    public boolean onQueryTextChange(String newText) {
        return false;
    }

    @Override
    protected void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        outState.putSerializable(SEARCH_TYPE_KEY, currentSearchType);
    }

    @Override
    public void setOngoing(boolean isOngoing, SearchType type) {
        if (this.currentSearchType == type) {
            progressBar.setVisibility(isOngoing ? View.VISIBLE : View.GONE);
            progressBar.setIndeterminate(isOngoing);
        }
    }

    @Override
    public void notifyNewsAdded(int startIndex, int count, SearchType type) {
        if (this.currentSearchType == type) {
            adapter.notifyItemRangeInserted(startIndex, count);
        }
    }

    @Override
    public void clearData(SearchType type) {
        if (this.currentSearchType == type) {
            if (adapter != null)
                adapter.notifyDataSetChanged();
        }
    }

    @Override
    public void showError(int messageId, SearchType type) {
        if (this.currentSearchType == type) {
            if (snackbar != null)
                snackbar.dismiss();

            snackbar = Snackbar.make(progressBar, messageId, Snackbar.LENGTH_LONG);
            snackbar.addCallback(snackbarCallback);
            snackbar.show();
        }
    }

    @Override
    public void hideError() {
        if (snackbar != null)
            snackbar.dismiss();
    }

    void tryInitializeRecent() {
        // Load recent news if it has not loaded yet
        if (currentSearchType == SearchType.Recent && presenter.getNewsCount(currentSearchType) == 0)
            presenter.getNextPage(currentSearchType);
    }


    @Override
    public Context getContext() {
        return getApplicationContext();
    }

    class NewsAdapter extends RecyclerView.Adapter<NewsListActivity.NewsHolder> {

        @Override
        public NewsListActivity.NewsHolder onCreateViewHolder(ViewGroup parent, int viewType) {
            View view = getLayoutInflater().inflate(R.layout.layout_newsitem_withimage, parent, false);
            return new NewsListActivity.NewsHolder(view);
        }

        @Override
        public void onBindViewHolder(NewsListActivity.NewsHolder holder, int position) {
            holder.bindView(position);
        }

        @Override
        public int getItemCount() {
            return presenter.getNewsCount(currentSearchType);
        }
    }

    class NewsHolder extends RecyclerView.ViewHolder {
        TextView title, author, date;
        ImageView media;

        NewsHolder(View itemView) {
            super(itemView);
            title = itemView.findViewById(R.id.title);
            media = itemView.findViewById(R.id.media);
            author = itemView.findViewById(R.id.author);
            date = itemView.findViewById(R.id.date);
        }

        void bindView(int position) {
            NewsItem item = presenter.getNewsItem(position, currentSearchType);
            if (item != null) {
                title.setText(item.getHeadline().getTitle());
                Credit credit = item.getCredit();
                if (credit != null)
                    author.setText(credit.getAuthor());
                date.setText(item.getPublicationDate());

                String mediaUri = item.getMediaUri();
                if (mediaUri != null) {
                    media.setVisibility(View.VISIBLE);
                    Picasso.with(NewsListActivity.this)
                            .load(Uri.parse(item.getMediaUri()))
                            .fit()
                            .centerCrop()
                            .into(media);
                } else
                    media.setVisibility(View.GONE);

                itemView.setOnClickListener(v -> {
                    Intent intent = new Intent(NewsListActivity.this, DetailsActivity.class);
                    intent.putExtra(DetailsActivity.ITEM_POSITION, position);
                    intent.putExtra(DetailsActivity.SEARCH_TYPE, currentSearchType);

                    startActivity(intent);
                });
            }
        }
    }
}
