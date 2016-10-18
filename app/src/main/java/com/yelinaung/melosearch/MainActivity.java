package com.yelinaung.melosearch;

import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.TextView;
import butterknife.BindView;
import butterknife.ButterKnife;
import com.bumptech.glide.Glide;
import com.google.gson.Gson;
import com.google.gson.JsonObject;
import com.google.gson.annotations.SerializedName;
import com.google.gson.reflect.TypeToken;
import com.jakewharton.rxbinding.widget.RxTextView;
import java.util.ArrayList;
import java.util.HashMap;
import retrofit2.Retrofit;
import retrofit2.adapter.rxjava.RxJavaCallAdapterFactory;
import retrofit2.converter.gson.GsonConverterFactory;
import retrofit2.http.GET;
import retrofit2.http.QueryMap;
import rx.Observable;
import rx.Subscriber;
import rx.android.schedulers.AndroidSchedulers;
import rx.functions.Action1;
import rx.functions.Func1;
import rx.schedulers.Schedulers;
import timber.log.Timber;

import static com.yelinaung.melosearch.R.id.rvAutocomplete;

public class MainActivity extends AppCompatActivity {

  @BindView(R.id.etAutoComplete) EditText etAutoComplete;
  @BindView(rvAutocomplete) RecyclerView rvAutoComplete;
  private AutoCompleteService autoCompleteService;

  private MeloAutoCompleteAdapter meloAutoCompleteAdapter;

  @Override protected void onCreate(Bundle savedInstanceState) {
    super.onCreate(savedInstanceState);
    setContentView(R.layout.activity_main);
    ButterKnife.bind(this);

    // initialise retrofit stuffs
    Retrofit retrofit =
        new Retrofit.Builder().addCallAdapterFactory(RxJavaCallAdapterFactory.create())
            .addConverterFactory(GsonConverterFactory.create())
            .baseUrl("https://melomap.com/home/") // wth camelCase in URL !?
            .build();

    autoCompleteService = retrofit.create(AutoCompleteService.class);

    // Recycler view stuffs
    meloAutoCompleteAdapter = new MeloAutoCompleteAdapter();
    LinearLayoutManager linearLayoutManager =
        new LinearLayoutManager(this, LinearLayoutManager.VERTICAL, false);
    rvAutoComplete.setLayoutManager(linearLayoutManager);
    rvAutoComplete.setAdapter(meloAutoCompleteAdapter);

    RxTextView.textChanges(etAutoComplete).filter(new Func1<CharSequence, Boolean>() {
      @Override public Boolean call(CharSequence charSequence) {
        if (charSequence.length() == 0) {
          Timber.d("search query is empty yo!");
          fallBackToDefault();
        }
        // return true only when query length is more than 0
        return charSequence.length() > 0;
      }
    }).doOnError(new Action1<Throwable>() {
      @Override public void call(Throwable throwable) {
        Timber.e(throwable, "Error in autocomplete, Sir. Sorry daddy-o");
      }
    }).observeOn(AndroidSchedulers.mainThread()).subscribe(new Action1<CharSequence>() {
      @Override public void call(CharSequence charSequence) {
        getSuggestions(charSequence.toString());
      }
    });
  }

  private void fallBackToDefault() {
    // reset the adapter
    ArrayList<AutoCompleteResult.Album> results = new ArrayList<>();
    meloAutoCompleteAdapter.setAutoCompleteResults(results);
  }

  private void getSuggestions(String query) {
    HashMap<String, String> queryParams = new HashMap<>();
    queryParams.put("keyword", query);
    Observable<JsonObject> autoCompleteObj = autoCompleteService.getAutoCompleteData(queryParams);
    autoCompleteObj.subscribeOn(Schedulers.newThread())
        .observeOn(AndroidSchedulers.mainThread())
        .subscribe(new Subscriber<JsonObject>() {
          @Override public void onCompleted() {
          }

          @Override public void onError(Throwable e) {
            Timber.e(e, "error searching in autocomplete");
          }

          @Override public void onNext(JsonObject jsonObject) {
            Timber.d("result %s", jsonObject.toString());
            Gson gson = new Gson();
            if (jsonObject.has("albums")) {
              ArrayList<AutoCompleteResult.Album> autocompleteResults =
                  gson.fromJson(jsonObject.get("albums").getAsJsonArray(),
                      new TypeToken<ArrayList<AutoCompleteResult.Album>>() {
                      }.getType());
              meloAutoCompleteAdapter.setAutoCompleteResults(autocompleteResults);
            }
          }
        });
  }

  public interface AutoCompleteService {
    @GET("autocompleteSearch") Observable<JsonObject> getAutoCompleteData(
        @QueryMap HashMap<String, String> queryParams);
  }

  public class MeloAutoCompleteAdapter extends RecyclerView.Adapter<RecyclerView.ViewHolder> {

    public ArrayList<AutoCompleteResult.Album> autoCompleteResults = new ArrayList<>();

    public void setAutoCompleteResults(ArrayList<AutoCompleteResult.Album> autoCompleteResults) {
      this.autoCompleteResults = autoCompleteResults;
      Timber.d("autocomplete size %d", autoCompleteResults.size());
      notifyDataSetChanged();
    }

    @Override public RecyclerView.ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
      View view = LayoutInflater.from(parent.getContext())
          .inflate(R.layout.row_autocomplete_suggestion, parent, false);
      return new AutoCompleteHolder(view);
    }

    @Override public void onBindViewHolder(RecyclerView.ViewHolder holder, int position) {
      if (holder instanceof AutoCompleteHolder) {
        AutoCompleteHolder autoCompleteHolder = (AutoCompleteHolder) holder;
        AutoCompleteResult.Album album = autoCompleteResults.get(position);

        if (album.albumImagePath != null && !album.albumImagePath.isEmpty()) {
          Glide.with(MainActivity.this)
              .load(album.albumImagePath)
              .centerCrop()
              .crossFade()
              .into(autoCompleteHolder.ivAutocompleteSuggestion);
        }

        if (album.albumName != null && !album.albumName.isEmpty()) {
          autoCompleteHolder.tvAutocompleteSuggestion.setText(album.albumName);
        }
      }
    }

    @Override public int getItemCount() {
      return autoCompleteResults.size();
    }

    public class AutoCompleteHolder extends RecyclerView.ViewHolder {

      @BindView(R.id.ivAutocompleteSuggestion) ImageView ivAutocompleteSuggestion;
      @BindView(R.id.tvAutocompleteSuggestion) TextView tvAutocompleteSuggestion;

      public AutoCompleteHolder(View itemView) {
        super(itemView);
        ButterKnife.bind(this, itemView);
      }
    }
  }

  public class AutoCompleteResult {
    @SerializedName("albums") ArrayList<Album> albums;

    public class Album {
      @SerializedName("album_id") public String albumId;
      @SerializedName("album_img_path") public String albumImagePath;
      @SerializedName("album_name") public String albumName;
    }
  }
}
