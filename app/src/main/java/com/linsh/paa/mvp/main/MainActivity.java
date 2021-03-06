package com.linsh.paa.mvp.main;

import android.support.v7.widget.GridLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.view.Gravity;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;

import com.linsh.lshapp.common.base.BaseToolbarHomeActivity;
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
import com.linsh.paa.task.network.Url;
import com.linsh.paa.tools.BeanHelper;
import com.linsh.paa.view.DisplayItemDialog;

import java.util.ArrayList;
import java.util.List;

import hugo.weaving.DebugLog;

public class MainActivity extends BaseToolbarHomeActivity<MainContract.Presenter>
        implements MainContract.View {

    private MainAdapter mAdapter;
    private BottomViewHelper mBottomViewHelper;
    private RecyclerView mRvContent;
    private GridLayoutManager mLayoutManager;
    private String lastClipboardText;

    @Override
    protected MainContract.Presenter initPresenter() {
        return new MainPresenter();
    }

    @Override
    protected String getToolbarTitle() {
        return "收藏夹";
    }

    @Override
    protected int getLayout() {
        return R.layout.activity_main;
    }

    @DebugLog
    @Override
    protected void initView() {
        // 底部栏
        mBottomViewHelper = new BottomViewHelper(this);
        mBottomViewHelper.setViewHelperListener(new BottomViewHelper.ViewHelperListener() {

            @DebugLog
            @Override
            public void delete() {
                ArrayList<String> selectedItemIds = mAdapter.getSelectedIds();
                if (selectedItemIds.size() == 0) {
                    showToast("请先选择宝贝~");
                    return;
                }
                showTextDialog("确认删除选择的宝贝?", null, dialog -> {
                    dialog.dismiss();
                    mPresenter.removeOrDeleteItems(selectedItemIds);
                }, null, null);
            }

            @DebugLog
            @Override
            public void move() {
                ArrayList<String> selectedItemIds = mAdapter.getSelectedIds();
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
        // 列表
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
                Item item = mAdapter.getData().get(position);
                LshActivityUtils.newIntent(AnalysisActivity.class)
                        .putExtra(item.getId())
                        .putExtra(item.getTitle(), 1)
                        .putExtra(item.getShopName(), 2)
                        .startActivity(getActivity());
            }

            @DebugLog
            @Override
            public void onItemLongClick(View view, int position) {
                String[] items = {"打开宝贝详情链接", "打开价格历史链接", "设置提醒价格", "设置正常价格",
                        mPresenter.isShowingRemoved() ? "重新关注" : "不再关注", "删除该宝贝"};
                new LshPopupWindow(MainActivity.this)
                        .BuildList()
                        .setItems(items, (window, index) -> {
                            window.dismiss();
                            Item item = mAdapter.getData().get(position);
                            switch (items[index]) {
                                case "打开宝贝详情链接":
                                    LshActivityUtils.newIntent(ItemDisplayActivity.class)
                                            .putExtra(item.getId())
                                            .startActivity(getActivity());
                                    break;
                                case "打开价格历史链接":
                                    String url = "http://tool.manmanbuy.com/historyLowest.aspx?url="
                                            + Url.getDetailHtmlUrl(item.getId());
                                    LshActivityUtils.newIntent(ItemDisplayActivity.class)
                                            .putExtra(url)
                                            .startActivity(getActivity());
                                    break;
                                case "设置提醒价格":
                                    int notifiedPrice = item.getNotifiedPrice();
                                    new LshColorDialog(getActivity())
                                            .buildInput()
                                            .setTitle("设置提醒价格")
                                            .setHint(BeanHelper.getPriceStr(notifiedPrice))
                                            .setPositiveButton(null, (dialog, inputText) -> {
                                                if (!inputText.matches("\\d+(.\\d+)?")) {
                                                    showToast("请输入正确的格式");
                                                    return;
                                                }
                                                dialog.dismiss();
                                                mPresenter.setNotifiedPrice(item.getId(), inputText);
                                            })
                                            .setNegativeButton(null, null)
                                            .show();
                                    break;
                                case "设置正常价格":
                                    int normalPrice = item.getNormalPrice();
                                    new LshColorDialog(getActivity())
                                            .buildInput()
                                            .setTitle("设置正常价格")
                                            .setHint(BeanHelper.getPriceStr(normalPrice))
                                            .setPositiveButton(null, (dialog, inputText) -> {
                                                if (!inputText.matches("\\d+(.\\d+)?")) {
                                                    showToast("请输入正确的格式");
                                                    return;
                                                }
                                                dialog.dismiss();
                                                mPresenter.setNormalPrice(item.getId(), inputText);
                                            })
                                            .setNegativeButton(null, null)
                                            .show();
                                    break;
                                case "重新关注":
                                    mPresenter.cancelRemoveItem(item.getId());
                                    break;
                                case "不再关注":
                                    mPresenter.removeItem(item.getId());
                                    break;
                                case "删除该宝贝":
                                    mPresenter.deleteItem(item.getId());
                                    break;
                            }
                        })
                        .getPopupWindow()
                        .showAtLocation(mRvContent, Gravity.CENTER, 0, 0);
            }

            @Override
            public void onPlatformSelected(String platform) {
                mPresenter.onPlatformSelected(platform);
            }

            @Override
            public void onTagSelected(String tag) {
                mPresenter.onTagSelected(tag);
            }

            @Override
            public void onStatusSelected(String status) {
                mPresenter.onStatusSelected(status);
            }
        });
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.main, menu);
        return true;
    }

    @DebugLog
    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        int id = item.getItemId();
        switch (id) {
            case R.id.menu_main_add_item:
                // 获取剪贴板的文字并检查
                String text = LshClipboardUtils.getText();
                if (LshStringUtils.isEquals(text, lastClipboardText)) {
                    showInputItemUrlDialog();
                } else {
                    lastClipboardText = text;
                    mPresenter.getItem(text, false);
                }
                return true;
            case R.id.menu_main_update_all:
                mPresenter.updateAll();
                return true;
            case R.id.menu_main_goto_taobao:
                LshActivityUtils.newIntent(ItemDisplayActivity.class)
                        .putExtra("https://www.taobao.com/")
                        .startActivity(this);
                return true;
            case R.id.menu_main_goto_jingdong:
                LshActivityUtils.newIntent(ItemDisplayActivity.class)
                        .putExtra("https://www.jd.com/")
                        .startActivity(this);
                return true;
            case R.id.menu_main_edit:
                mBottomViewHelper.showBottom(this, !mPresenter.isShowingRemoved());
                mAdapter.setSelectMode(true);
                return true;
            case R.id.menu_main_setting:
                LshActivityUtils.newIntent(SettingsActivity.class)
                        .startActivity(this);
                return true;
        }
        return super.onOptionsItemSelected(item);
    }

    @Override
    public void onBackPressed() {
        if (mBottomViewHelper.isShowing()) {
            mBottomViewHelper.dismissBottom();
            return;
        }
        super.onBackPressed();
    }

    @Override
    public void showInputItemUrlDialog() {
        new LshColorDialog(this)
                .buildInput()
                .setTitle("添加宝贝")
                .setHint("请输入宝贝链接")
                .setPositiveButton(null, (lshColorDialog, text) -> {
                    lshColorDialog.dismiss();
                    mPresenter.getItem(text, true);
                })
                .setNegativeButton(null, null)
                .show();
    }

    @DebugLog
    @Override
    public void showItem(Object[] toSave, boolean isConfirm) {
        new DisplayItemDialog(getActivity())
                .setData((Item) toSave[0])
                .setOnPositiveClickListener(dialog -> {
                    dialog.dismiss();
                    mPresenter.saveItem(toSave);
                })
                .setNegativeText(isConfirm ? "取消" : "手动输入")
                .setOnNegativeClickListener(dialog -> {
                    dialog.dismiss();
                    if (!isConfirm) {
                        showInputItemUrlDialog();
                    }
                })
                .show();
    }

    @DebugLog
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
