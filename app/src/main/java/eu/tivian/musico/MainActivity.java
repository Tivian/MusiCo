package eu.tivian.musico;

import android.annotation.SuppressLint;
import android.content.Intent;
import android.os.Bundle;
import android.view.ContextMenu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewPropertyAnimator;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.lifecycle.ViewModelProvider;
import androidx.viewpager2.adapter.FragmentStateAdapter;
import androidx.viewpager2.widget.ViewPager2;

import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.google.android.material.tabs.TabLayout;
import com.google.android.material.tabs.TabLayoutMediator;
import com.google.zxing.integration.android.IntentIntegrator;
import com.google.zxing.integration.android.IntentResult;

import eu.tivian.musico.data.Album;
import eu.tivian.musico.database.DatabaseAdapter;
import eu.tivian.musico.net.Discogs;
import eu.tivian.musico.ui.CustomScannerActivity;
import eu.tivian.musico.ui.ScreenSlidePagerAdapter;
import eu.tivian.musico.utility.Utilities;

/**
 * Main activity of the application.
 */
public class MainActivity extends AppCompatActivity {
    /**
     * The tag used to distinguish which context menu was called.
     */
    private static final int CONTEXT_MENU_TAG = 0x3141;

    /**
     * The database adapter.
     */
    private DatabaseAdapter databaseAdapter;

    /**
     * The view model containing data shared between fragments and activities.
     */
    private SharedViewModel viewModel;

    /**
     * Tab layout mediator, which provides names of the tabs.
     */
    private TabLayoutMediator mediator;

    /**
     * The button used to extend menu to choose between two possible modes for adding new albums.
     */
    private FloatingActionButton fab;

    /**
     * The button used to add new albums using the barcode scanner.
     */
    private FloatingActionButton fabCamera;

    /**
     * The button used to add new albums manually.
     */
    private FloatingActionButton fabManual;

    /**
     * The view used to 'greyout' rest of the screen.
     */
    private View greyout;

    /**
     * Opens the album adding menu.
     */
    @SuppressLint("ClickableViewAccessibility")
    private void menuOpen() {
        greyout.animate().alpha(0.5f);
        fab.animate().rotation(45);
        fabCamera.animate().setStartDelay( 75).alpha(1)
            .withStartAction(() -> fabCamera.setVisibility(View.VISIBLE));
        fabManual.animate().setStartDelay(125).alpha(1)
            .withStartAction(() -> fabManual.setVisibility(View.VISIBLE));
        greyout.setOnTouchListener((vi, event) -> {
            menuClose();
            return true;
        });

        fab.setOnClickListener(v -> menuClose());
    }

    /**
     * Closes the album adding menu.
     */
    private void menuClose() {
        greyout.setOnTouchListener(null);
        fab.animate().rotation( 0);
        fabCamera.animate().setStartDelay(125).alpha(0)
            .withEndAction(() -> fabCamera.setVisibility(View.GONE));
        fabManual.animate().setStartDelay( 75).alpha(0)
            .withEndAction(() -> fabManual.setVisibility(View.GONE));
        greyout.animate().alpha(0);

        fab.setOnClickListener(v -> menuOpen());
    }

    /**
     * Creates and initializes the UI for the activity.
     *
     * @param savedInstanceState if the activity is being re-initialized after previously being
     *        shut down then this Bundle contains the data it most recently supplied in
     *        {@link #onSaveInstanceState(Bundle)}. This value may be {@code null}.
     */
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        ViewPager2 viewPager = findViewById(R.id.view_pager);
        TabLayout tabs = findViewById(R.id.tabs);
        fab = findViewById(R.id.fab);
        fabCamera = findViewById(R.id.fab_camera);
        fabManual = findViewById(R.id.fab_manual);
        greyout = findViewById(R.id.view_grayout);

        databaseAdapter = DatabaseAdapter.init(getApplicationContext());
        databaseAdapter.updateRates();

        viewModel = new ViewModelProvider(this).get(SharedViewModel.class);
        viewModel.getFabAlpha().observe(this, alpha -> {
            ViewPropertyAnimator animator = fab.animate().alpha(alpha);
            if (alpha == 1)
                animator.withStartAction(() -> fab.setVisibility(View.VISIBLE));
            else
                animator.withEndAction(() -> fab.setVisibility(View.GONE));
        });
        viewModel.getLanguage().observe(this, lang -> {
            Utilities.setLocale(this, lang);
            if (mediator != null) {
                mediator.detach();
                mediator.attach();
            }
        });

        FragmentStateAdapter pagerAdapter =
                new ScreenSlidePagerAdapter(getSupportFragmentManager(), getLifecycle());
        viewPager.setAdapter(pagerAdapter);
        viewPager.setOffscreenPageLimit(tabs.getTabCount() - 1);
        viewPager.registerOnPageChangeCallback(new ViewPager2.OnPageChangeCallback() {
            @Override
            public void onPageSelected(int position) {
                menuClose();
                viewModel.setFabAlpha(position == 0 ? 1 : 0);
            }
        });

        mediator = new TabLayoutMediator(tabs, viewPager,
            (tab, position) -> tab.setText(ScreenSlidePagerAdapter.TAB_TITLE[position])
        );
        mediator.attach();

        menuClose();
        fabCamera.setOnClickListener(v -> {
            fab.callOnClick();
            new IntentIntegrator(this)
                .setCaptureActivity(CustomScannerActivity.class)
                .setBeepEnabled(false)
                .initiateScan();
        });

        fabManual.setOnClickListener(v -> {
            fab.callOnClick();
            startActivityForResult(new Intent(this, AlbumView.class)
                .putExtra(AlbumView.MODE_KEY, AlbumView.MODE_ADD), AlbumView.REQUEST_CODE);
        });

        TabLayout.Tab tab = tabs.getTabAt(0);
        if (tab != null) {
            registerForContextMenu(tab.view);
            tab.view.setTag(CONTEXT_MENU_TAG);
        }
    }

    /**
     * Creates context menu used to choose the ordering of the album list.
     *
     * @param menu the context menu that is being built.
     * @param v the view for which the context menu is being built.
     * @param menuInfo always {@code null}.
     */
    // FIXME
    //  this menu doesn't show up on API lesser then 26
    @Override
    public void onCreateContextMenu(@NonNull ContextMenu menu, @NonNull View v,
                                    @Nullable ContextMenu.ContextMenuInfo menuInfo) {
        super.onCreateContextMenu(menu, v, menuInfo);

        if (v.getTag() == null || !v.getTag().equals(CONTEXT_MENU_TAG))
            return;

        new MenuInflater(getApplicationContext()).inflate(R.menu.context_menu_sort, menu);
        menu.setHeaderTitle(R.string.menu_sort_title);
    }

    /**
     * Sets the ordering of the album list according to the chosen option.
     *
     * @param item the context menu item that was selected. This value cannot be null.
     * @return return false to allow normal context menu processing to proceed, true to consume it here.
     */
    @Override
    public boolean onContextItemSelected(@Nullable MenuItem item) {
        if (item == null)
            return false;

        switch (item.getItemId()) {
            case R.id.menu_sort_album:
                viewModel.setCursor(databaseAdapter.getCursor(
                        DatabaseAdapter.Sort.TITLE, DatabaseAdapter.Sort.YEAR));
                return true;
            case R.id.menu_sort_artist:
                viewModel.setCursor(databaseAdapter.getCursor(
                        DatabaseAdapter.Sort.ARTIST, DatabaseAdapter.Sort.YEAR));
                return true;
            case R.id.menu_sort_year:
                viewModel.setCursor(databaseAdapter.getCursor(
                        DatabaseAdapter.Sort.YEAR, DatabaseAdapter.Sort.TITLE));
                return true;
            case R.id.menu_sort_date:
                viewModel.setCursor(databaseAdapter.getCursor(
                        DatabaseAdapter.Sort.DATE, DatabaseAdapter.Sort.TITLE));
                return true;
            case R.id.menu_sort_price:
                viewModel.setCursor(databaseAdapter.getCursor(
                        DatabaseAdapter.Sort.PRICE, DatabaseAdapter.Sort.DATE));
                return true;
            default:
                return super.onContextItemSelected(item);
        }
    }

    /**
     * Based on the {@code requestCode} it either opens the album editor or
     *  adds the new album into the database.
     *
     * @param requestCode the integer request code originally supplied to
     *        {@link #startActivityForResult(Intent, int)} allowing to identify the caller.
     * @param resultCode the integer result code returned by the child activity through its {@link #setResult(int)}.
     * @param data an {@link Intent}, which can return result data to the caller.
     */
    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        IntentResult result = IntentIntegrator.parseActivityResult(requestCode, resultCode, data);

        if (result != null) {
            if (result.getContents() == null) {
                Toast.makeText(this, getText(R.string.scan_cancel), Toast.LENGTH_LONG).show();
            } else { // opens album editor
                Discogs.get().getAlbum(result.getContents(),
                        album -> startActivityForResult(
                                new Intent(this, AlbumView.class)
                                    .putExtra(AlbumView.ALBUM_KEY, album)
                                    .putExtra(AlbumView.MODE_KEY, AlbumView.MODE_ADD),
                                AlbumView.REQUEST_CODE),
                        ex -> Toast.makeText(this, getText(R.string.scan_error), Toast.LENGTH_LONG).show());
            }
        } else if (data != null && requestCode == AlbumView.REQUEST_CODE) {
            Album album = data.getParcelableExtra(AlbumView.ALBUM_KEY);
            if (album != null) { // adds the album
                databaseAdapter.add(album);
                viewModel.setCursor(databaseAdapter.getCursor(true));
                Toast.makeText(this, R.string.msg_album_add, Toast.LENGTH_LONG).show();
            }
        } else {
            super.onActivityResult(requestCode, resultCode, data);
        }
    }
}
