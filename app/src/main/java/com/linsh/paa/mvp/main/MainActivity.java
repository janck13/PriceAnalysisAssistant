package com.linsh.paa.mvp.main;

import android.support.v7.widget.GridLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.view.Gravity;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;

import com.linsh.lshapp.common.base.BaseViewActivity;
import com.linsh.lshapp.common.view.LshPopupWindow;
import com.linsh.lshutils.utils.Basic.LshStringUtils;
import com.linsh.lshutils.utils.LshActivityUtils;
import com.linsh.lshutils.utils.LshClipboardUtils;
import com.linsh.lshutils.view.LshColorDialog;
import com.linsh.paa.R;
import com.linsh.paa.model.bean.db.Item;
import com.linsh.paa.mvp.analysis.AnalysisActivity;
import com.linsh.paa.mvp.display.ItemDisplayActivity;
import com.linsh.paa.mvp.setting.SettingsActivity;

import java.util.ArrayList;
import java.util.List;

public class MainActivity extends BaseViewActivity<MainContract.Presenter>
        implements MainContract.View {

    private MainAdapter mAdapter;
    private BottomViewHelper mBottomViewHelper;
    private RecyclerView mRvContent;
    private GridLayoutManager mLayoutManager;

    @Override
    protected MainContract.Presenter initPresenter() {
        return new MainPresenter();
    }

    @Override
    protected int getLayout() {
        return R.layout.activity_main;
    }

    @Override
    protected void initView() {
        getSupportActionBar().setTitle("价格分析助手");
        mBottomViewHelper = new BottomViewHelper(this);
        mBottomViewHelper.setViewHelperListener(new BottomViewHelper.ViewHelperListener() {
            @Override
            public void delete() {
                ArrayList<String> selectedItemIds = mAdapter.getSelectedItemIds();
                if (selectedItemIds.size() == 0) {
                    showToast("请先选择宝贝~");
                    return;
                }
                showTextDialog("确认删除选择的宝贝?", null, dialog -> {
                    dialog.dismiss();
                    mPresenter.deleteItems(selectedItemIds);
                }, null, null);
            }

            @Override
            public void move() {
                ArrayList<String> selectedItemIds = mAdapter.getSelectedItemIds();
                if (selectedItemIds.size() == 0) {
                    showToast("请先选择宝贝~");
                    return;
                }
                List<String> tags = mPresenter.getTags();
                tags.add("+ 添加标签");
                new LshPopupWindow(getActivity())
                        .BuildList()
                        .setItems(tags, (window, index) -> {
                            window.dismiss();
                            if (index == tags.size() - 1) {
                                new LshColorDialog(getActivity())
                                        .buildInput()
                                        .setTitle("添加标签")
                                        .setPositiveButton("添加", (dialog, inputText) -> {
                                            if (LshStringUtils.isEmpty(inputText)) {
                                                showToast("标签不能为空");
                                                return;
                                            }
                                            dialog.dismiss();
                                            mPresenter.addTag(inputText, selectedItemIds);
                                        })
                                        .setNegativeButton(null, null)
                                        .show();
                            } else {
                                mPresenter.moveItemsToOtherTag(tags.get(index), selectedItemIds);
                            }
                        })
                        .getPopupWindow()
                        .showAtLocation(mRvContent, Gravity.CENTER, 0, 0);
            }

            @Override
            public void selectAll(boolean selected) {
                mAdapter.selectAll(selected);
            }

            @Override
            public void done() {
                mAdapter.setSelectMode(false);
            }
        });
        mRvContent = (RecyclerView) findViewById(R.id.rv_main_content);
        mLayoutManager = new GridLayoutManager(this, 2);
        mLayoutManager.setSpanSizeLookup(new GridLayoutManager.SpanSizeLookup() {
            @Override
            public int getSpanSize(int position) {
                return position == 0 ? mLayoutManager.getSpanCount() : 1;
            }
        });
        mRvContent.setLayoutManager(mLayoutManager);
        mAdapter = new MainAdapter();
        mRvContent.setAdapter(mAdapter);
        mAdapter.setOnMainAdapterListener(new MainAdapter.OnMainAdapterListener() {
            @Override
            public void onItemClick(View itemView, int position) {
                LshActivityUtils.newIntent(AnalysisActivity.class)
                        .putExtra(mAdapter.getData().get(position).getId())
                        .startActivity(getActivity());
            }

            @Override
            public void onItemLongClick(View view, int position) {
                new LshPopupWindow(MainActivity.this)
                        .BuildList()
                        .setItems(new String[]{"打开淘宝链接", "删除该宝贝"}, (window, index) -> {
                            window.dismiss();
                            switch (index) {
                                case 0:
                                    LshActivityUtils.newIntent(ItemDisplayActivity.class)
                                            .putExtra(mAdapter.getData().get(position).getId())
                                            .startActivity(getActivity());
                                    break;
                                case 1:
                                    mPresenter.deleteItem(mAdapter.getData().get(position).getId());
                                    break;
                            }
                        })
                        .getPopupWindow()
                        .showAtLocation(mRvContent, Gravity.CENTER, 0, 0);
            }

            @Override
            public void onTagSelected(String tag) {
                mPresenter.onTagSelected(tag);
            }
        });
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.main, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        int id = item.getItemId();
        switch (id) {
            case R.id.menu_main_add_item:
                String itemId = mPresenter.checkItem(LshClipboardUtils.getText());
                if (itemId != null) {
                    showTextDialog("检测到剪贴板中的宝贝(Id:" + itemId + "), 是否添加", "添加", lshColorDialog -> {
                        lshColorDialog.dismiss();
                        mPresenter.addItem(itemId);
                    }, null, null);
                } else {
                    showAddItemDialog();
                }
                return true;
            case R.id.menu_main_update_all:
                mPresenter.updateAll();
                return true;
            case R.id.menu_main_edit:
                mBottomViewHelper.showBottom(this);
                mAdapter.setSelectMode(true);
                return true;
            case R.id.menu_main_setting:
                LshActivityUtils.newIntent(SettingsActivity.class)
                        .startActivity(this);
                return true;
        }
        return super.onOptionsItemSelected(item);
    }

    private LshColorDialog showAddItemDialog() {
        return new LshColorDialog(this)
                .buildInput()
                .setTitle("添加宝贝")
                .setHint("请输入宝贝id 或者宝贝链接")
                .setPositiveButton(null, (lshColorDialog, text) -> {
                    lshColorDialog.dismiss();
                    String itemId = mPresenter.checkItem(text);
                    if (itemId != null) {
                        mPresenter.addItem(itemId);
                    } else {
                        showTextDialog("无法解析该宝贝, 请传入正确格式");
                    }
                })
                .setNegativeButton(null, null)
                .show();
    }

    @Override
    public void setData(List<Item> items) {
        mBottomViewHelper.resetSelectAll();
        if (items.size() == 0 && mLayoutManager.getSpanCount() != 1) {
            mLayoutManager.setSpanCount(1);
        } else if (items.size() > 0 && mLayoutManager.getSpanCount() != 2) {
            mLayoutManager.setSpanCount(2);
        }
        mAdapter.setData(items);
    }

    @Override
    public void setTags(List<String> tags) {
        mAdapter.setTags(tags);
    }
}
