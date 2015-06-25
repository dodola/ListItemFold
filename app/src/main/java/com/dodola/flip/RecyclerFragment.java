package com.dodola.flip;

import android.app.Activity;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AbsListView;
import android.widget.AdapterView;

import com.dodola.flip.dummy.ItemDataAdapter;
import com.dodola.flip.dummy.RecyclerDataAdapter;
import com.dodola.flip.dummy.SimpleData;
import com.loopj.android.http.AsyncHttpClient;
import com.loopj.android.http.JsonHttpResponseHandler;

import org.apache.http.Header;
import org.json.JSONArray;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.List;

/**
 * A fragment representing a list of Items.
 * <p/>
 * Large screen devices (such as tablets) are supported by replacing the ListView
 * with a GridView.
 * <p/>
 * Activities containing this fragment MUST implement the {@link OnFragmentInteractionListener}
 * interface.
 */
public class RecyclerFragment extends Fragment implements RecyclerDataAdapter.IOnRecyclerItemClick {

    private OnFragmentInteractionListener mListener;
    private RecyclerView mRecyclerView;
    private RecyclerDataAdapter mAdapter;

    // TODO: Rename and change types of parameters
    public static RecyclerFragment newInstance() {
        RecyclerFragment fragment = new RecyclerFragment();
        Bundle args = new Bundle();
        fragment.setArguments(args);
        return fragment;
    }

    /**
     * Mandatory empty constructor for the fragment manager to instantiate the
     * fragment (e.g. upon screen orientation changes).
     */
    public RecyclerFragment() {
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        if (getArguments() != null) {
        }

    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_item_list, container, false);
        mAdapter = new RecyclerDataAdapter(getActivity());
        mRecyclerView = (RecyclerView) view.findViewById(R.id.recycler);
        LinearLayoutManager layoutManager = new LinearLayoutManager(getActivity(), LinearLayoutManager.VERTICAL, false);
        mRecyclerView.setLayoutManager(layoutManager);
        mRecyclerView.setAdapter(mAdapter);
        mAdapter.setOnItemClick(this);
        DetailAnimViewGroup wrapper = new DetailAnimViewGroup(inflater.getContext(), view, 0);
        loadData();
        return wrapper;
    }

    @Override
    public void onAttach(Activity activity) {
        super.onAttach(activity);
        try {
            mListener = (OnFragmentInteractionListener) activity;
        } catch (ClassCastException e) {
            throw new ClassCastException(activity.toString()
                    + " must implement OnFragmentInteractionListener");
        }
    }

    @Override
    public void onDetach() {
        super.onDetach();
        mListener = null;
    }

    public void onItemClick(SimpleData data, View view) {
        if (null != mListener) {
            mListener.onFragmentInteraction(data, view);
        }
    }


    /**
     * This interface must be implemented by activities that contain this
     * fragment to allow an interaction in this fragment to be communicated
     * to the activity and potentially other fragments contained in that
     * activity.
     * <p/>
     * See the Android Training lesson <a href=
     * "http://developer.android.com/training/basics/fragments/communicating.html"
     * >Communicating with Other Fragments</a> for more information.
     */
    public interface OnFragmentInteractionListener {
        // TODO: Update argument type and name
        public void onFragmentInteraction(SimpleData data, View view);
    }

    private void loadData() {
        AsyncHttpClient client = new AsyncHttpClient();

        client.get(getActivity(), "https://moment.douban.com/api/stream/date/2015-06-09", new JsonHttpResponseHandler() {
            @Override
            public void onSuccess(int statusCode, Header[] headers, JSONObject response) {
                super.onSuccess(statusCode, headers, response);
                if (response != null) {
                    final JSONArray posts = response.optJSONArray("posts");
                    int length = posts.length();
                    List<SimpleData> resultDatas = new ArrayList<SimpleData>(length);
                    for (int i = 0; i < length; i++) {
                        JSONObject obj = posts.optJSONObject(i);
                        SimpleData data = new SimpleData();
                        data.content = obj.optString("abstract");
                        data.title = obj.optString("title");
                        data.url = obj.optString("url");
                        JSONArray thumbs = obj.optJSONArray("thumbs");
                        if (thumbs.length() > 0) {
                            JSONObject thumb = thumbs.optJSONObject(0);
                            thumb = thumb.optJSONObject("large");
                            if (thumb != null) {
                                data.picUrl = thumb.optString("url");
                                resultDatas.add(data);
                            }
                        }
                    }
                    mAdapter.addAll(resultDatas);
                }
            }
        });
    }
}
