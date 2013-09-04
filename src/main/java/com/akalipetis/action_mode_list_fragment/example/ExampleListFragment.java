/*
 * The MIT License (MIT)
 *
 * Copyright (c) 2013 Antonis Kalipetis <akalipetis@gmail.com>.
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in
 * all copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN
 * THE SOFTWARE.
 */

package com.akalipetis.action_mode_list_fragment.example;

import android.os.Bundle;
import android.support.v7.view.ActionMode;
import android.util.Log;
import android.util.SparseBooleanArray;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.AbsListView;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.ListView;
import android.widget.Toast;

import com.akalipetis.action_mode_list_fragment.ActionModeListFragment;
import com.akalipetis.action_mode_list_fragment.MultiChoiceModeListener;

import java.util.ArrayList;
import java.util.Random;

import de.timroes.swipetodismiss.SwipeDismissList;

/**
 * Created by Antonis Kalipetis on 31.07.2013.
 */
public class ExampleListFragment extends ActionModeListFragment implements MultiChoiceModeListener {
    private ArrayAdapter<String> mAdapter;
    private int mCnt;
    private ActionMode mMode;

    private SwipeDismissList mSwipeList;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        mAdapter = new ArrayAdapter<String>(getActivity(), R.layout.list_item_checkable, R.id.list_item_checkable_text);
        Random r = new Random();
        for (int i = 0; i < 100; ++i) {
            mAdapter.add(String.valueOf(i));
        }
        setMultiChoiceModeListener(this);

    }

    @Override
    public void onViewCreated(View view, Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        setListAdapter(mAdapter);
        getListView().setChoiceMode(AbsListView.CHOICE_MODE_NONE);
        getListView().setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                String item = mAdapter.getItem(position);
                Toast.makeText(getActivity(), "clicked on item " + item + " position: " + position + " id: " + id, Toast.LENGTH_SHORT).show();
            }
        });

        mSwipeList = new SwipeDismissList(getListView(), new SwipeDismissList.OnDismissCallback() {
            @Override
            public SwipeDismissList.Undoable onDismiss(AbsListView listView, final int position) {
                final String item = mAdapter.getItem(position);
                mAdapter.remove(item);
                return createUndoAction(item, position);
            }
        }, SwipeDismissList.UndoMode.COLLAPSED_UNDO);
    }

    @Override
    public void onItemCheckedStateChanged(ActionMode actionMode, int position, long id, boolean checked) {
        Log.d(ExampleListFragment.class.getSimpleName(), "onItemCheckedStateChanged");
        if (checked) mCnt++;
        else mCnt--;
        switch (mCnt) {
            case 1:
            case 2:
                actionMode.invalidate();
                break;
        }
    }

    @Override
    public boolean onCreateActionMode(ActionMode actionMode, Menu menu) {
        Log.d(ExampleListFragment.class.getSimpleName(), "onCreateActionMode");
        mCnt = 0;
        mMode = actionMode;
        return true;
    }

    @Override
    public boolean onPrepareActionMode(ActionMode actionMode, Menu menu) {
        Log.d(ExampleListFragment.class.getSimpleName(), "onPrepareActionMode");
        switch (mCnt) {
            case 1:
                menu.clear();
                getActivity().getMenuInflater().inflate(R.menu.fragment_example_list, menu);
                break;
            case 2:
                menu.clear();
                getActivity().getMenuInflater().inflate(R.menu.fragment_example_list_multiple, menu);
                break;
        }
        return true;
    }

    @Override
    public boolean onActionItemClicked(ActionMode actionMode, MenuItem item) {
        Log.d(ExampleListFragment.class.getSimpleName(), "onActionItemClicked");
        switch (item.getItemId()) {
            case R.id.menu_fragment_example_list_select_all:
                selectAll();
                break;
            case R.id.menu_fragment_example_list_delete:
                deleteSelected();
                break;
        }
        return true;
    }

    @Override
    public void onDestroyActionMode(ActionMode actionMode) {
        Log.d(ExampleListFragment.class.getSimpleName(), "onDestroyActionMode");
        mMode = null;
    }

    private void selectAll() {
        ListView list = getListView();
        int cnt = list.getAdapter().getCount();
        Log.d(ExampleListFragment.class.getSimpleName(), "Count is: " + cnt);
        for (int i = 0, lim = list.getAdapter().getCount(); i < lim; ++i)
            list.setItemChecked(i, true);
        Log.d(ExampleListFragment.class.getSimpleName(), "Checked count is: " + calculateCheckedItems());
    }

    private void deleteSelected() {
        ListView list = getListView();
        SparseBooleanArray checked = list.getCheckedItemPositions();
        ArrayList<String> deleted = new ArrayList<String>();
        for (int i = 0, lim = list.getAdapter().getCount(); i < lim; ++i)
            if (checked.get(i)) {
                deleted.add(mAdapter.getItem(i));
            }
        for (String s : deleted) {
            int pos = mAdapter.getPosition(s);
            mAdapter.remove(s);
            mSwipeList.queueUndoAction(createUndoAction(s, pos));
        }
        mAdapter.notifyDataSetChanged();
        mMode.finish();
        mSwipeList.showUndoPopup();

    }

    private SwipeDismissList.Undoable createUndoAction(final String item, final int position) {
        return  new SwipeDismissList.Undoable() {

            @Override
            public String getTitle() {
                return item + " deleted";
            }

            @Override
            public void undo() {
                // Reinsert the item at its previous position.
                mAdapter.insert(item, position);
                Log.d("UNDO", "inserting " + item + " into position " + position);
            }


            @Override
            public void discard() {
                // Just write a log message (use logcat to see the effect)
                Log.w("DISCARD", "item " + item + " now finally discarded");
            }
        };
    }

    @Override
    public void onStop() {
        mSwipeList.discardUndo();
        super.onStop();
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();

    }
}
