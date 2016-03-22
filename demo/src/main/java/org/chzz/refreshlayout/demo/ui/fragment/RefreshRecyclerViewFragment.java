package org.chzz.refreshlayout.demo.ui.fragment;

import android.os.Bundle;
import android.os.Handler;
import android.support.v7.widget.GridLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.util.Log;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import java.util.List;

import cn.bingoogolapple.androidcommon.adapter.BGAOnItemChildClickListener;
import cn.bingoogolapple.androidcommon.adapter.BGAOnItemChildLongClickListener;
import cn.bingoogolapple.androidcommon.adapter.BGAOnRVItemClickListener;
import cn.bingoogolapple.androidcommon.adapter.BGAOnRVItemLongClickListener;

import org.chzz.refreshlayout.demo.adapter.NormalRecyclerViewAdapter;
import org.chzz.refreshlayout.CHZZRefreshLayout;
import org.chzz.refreshlayout.CHZZStickinessRefreshViewHolder;
import org.chzz.refreshlayout.demo.R;

import org.chzz.refreshlayout.demo.model.RefreshModel;
import org.chzz.refreshlayout.demo.ui.activity.MainActivity;
import org.chzz.refreshlayout.demo.util.ThreadUtil;
import org.chzz.refreshlayout.demo.util.ToastUtil;
import org.chzz.refreshlayout.demo.widget.Divider;
import retrofit2.Callback;
import retrofit2.Response;

/**
 * 作者:copy 邮件:2499551993@qq.com
 * 创建时间:15/5/22 10:06
 * 描述:
 */
public class RefreshRecyclerViewFragment extends BaseFragment implements CHZZRefreshLayout.BGARefreshLayoutDelegate, BGAOnRVItemClickListener, BGAOnRVItemLongClickListener, BGAOnItemChildClickListener, BGAOnItemChildLongClickListener {
    private static final String TAG = RefreshRecyclerViewFragment.class.getSimpleName();
    private NormalRecyclerViewAdapter mAdapter;
    private CHZZRefreshLayout mRefreshLayout;
    private RecyclerView mDataRv;
    private int mNewPageNumber = 0;
    private int mMorePageNumber = 0;

    @Override
    protected void initView(Bundle savedInstanceState) {
        setContentView(R.layout.fragment_recyclerview_refresh);
        mRefreshLayout = getViewById(R.id.rl_recyclerview_refresh);
        mDataRv = getViewById(R.id.rv_recyclerview_data);
    }

    @Override
    protected void setListener() {
        mRefreshLayout.setDelegate(this);

        mAdapter = new NormalRecyclerViewAdapter(mDataRv);
        mAdapter.setOnRVItemClickListener(this);
        mAdapter.setOnRVItemLongClickListener(this);
        mAdapter.setOnItemChildClickListener(this);
        mAdapter.setOnItemChildLongClickListener(this);

        // 使用addOnScrollListener，而不是setOnScrollListener();
        mDataRv.addOnScrollListener(new RecyclerView.OnScrollListener() {
            @Override
            public void onScrollStateChanged(RecyclerView recyclerView, int newState) {
                Log.i(TAG, "测试自定义onScrollStateChanged被调用");
            }

            @Override
            public void onScrolled(RecyclerView recyclerView, int dx, int dy) {
                Log.i(TAG, "测试自定义onScrolled被调用");
            }
        });
    }

    @Override
    protected void processLogic(Bundle savedInstanceState) {
//        mRefreshLayout.setCustomHeaderView(DataEngine.getCustomHeaderView(mApp), true);

        View headerView = View.inflate(mApp, R.layout.view_custom_header2, null);

        // 测试自定义header中控件的点击事件
        headerView.findViewById(R.id.btn_custom_header2_test).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                ToastUtil.show("点击了测试按钮");
            }
        });
        // 模拟网络数据加载，测试动态改变自定义header的高度
        new Handler().postDelayed(new Runnable() {
            @Override
            public void run() {
                ((TextView) getViewById(R.id.tv_custom_header2_title)).setText(R.string.test_custom_header_title);
                ((TextView) getViewById(R.id.tv_custom_header2_desc)).setText(R.string.test_custom_header_desc);
            }
        }, 2000);
        mRefreshLayout.setCustomHeaderView(headerView, true);

        CHZZStickinessRefreshViewHolder stickinessRefreshViewHolder = new CHZZStickinessRefreshViewHolder(mApp, true);
        stickinessRefreshViewHolder.setStickinessColor(R.color.colorPrimary);
        stickinessRefreshViewHolder.setRotateImage(R.mipmap.bga_refresh_stickiness);
        mRefreshLayout.setRefreshViewHolder(stickinessRefreshViewHolder);

        mDataRv.addItemDecoration(new Divider(mApp));

        mDataRv.setLayoutManager(new GridLayoutManager(mApp, 2, GridLayoutManager.VERTICAL, false));
//        mDataRv.setLayoutManager(new LinearLayoutManager(mApp, LinearLayoutManager.VERTICAL, false));

        mDataRv.setAdapter(mAdapter);
    }

    @Override
    protected void onUserVisible() {
        mNewPageNumber = 0;
        mMorePageNumber = 0;
        mEngine.loadInitDatas().enqueue(new Callback<List<RefreshModel>>() {
            @Override
            public void onResponse(Response<List<RefreshModel>> response) {
                mAdapter.setDatas(response.body());
            }

            @Override
            public void onFailure(Throwable t) {
            }
        });
    }

    @Override
    public void onBGARefreshLayoutBeginRefreshing(CHZZRefreshLayout refreshLayout) {
        mNewPageNumber++;
        if (mNewPageNumber > 4) {
            mRefreshLayout.endRefreshing();
            showToast("没有最新数据了");
            return;
        }

        showLoadingDialog();
        mEngine.loadNewData(mNewPageNumber).enqueue(new Callback<List<RefreshModel>>() {
            @Override
            public void onResponse(final Response<List<RefreshModel>> response) {
                ThreadUtil.runInUIThread(new Runnable() {
                    @Override
                    public void run() {
                        mRefreshLayout.endRefreshing();
                        dismissLoadingDialog();
                        mAdapter.addNewDatas(response.body());
                        mDataRv.smoothScrollToPosition(0);
                    }
                }, MainActivity.LOADING_DURATION);
            }

            @Override
            public void onFailure(Throwable t) {
                mRefreshLayout.endRefreshing();
                dismissLoadingDialog();
            }
        });
    }

    @Override
    public boolean onBGARefreshLayoutBeginLoadingMore(CHZZRefreshLayout refreshLayout) {
        mMorePageNumber++;
        if (mMorePageNumber > 4) {
            mRefreshLayout.endLoadingMore();
            showToast("没有更多数据了");
            return false;
        }

        showLoadingDialog();
        mEngine.loadMoreData(mMorePageNumber).enqueue(new Callback<List<RefreshModel>>() {
            @Override
            public void onResponse(final Response<List<RefreshModel>> response) {
                ThreadUtil.runInUIThread(new Runnable() {
                    @Override
                    public void run() {
                        mRefreshLayout.endLoadingMore();
                        dismissLoadingDialog();
                        mAdapter.addMoreDatas(response.body());
                    }
                }, MainActivity.LOADING_DURATION);
            }

            @Override
            public void onFailure(Throwable t) {
                mRefreshLayout.endLoadingMore();
                dismissLoadingDialog();
            }
        });

        return true;
    }

    @Override
    public void onItemChildClick(ViewGroup parent, View childView, int position) {
        if (childView.getId() == R.id.tv_item_normal_delete) {
            mAdapter.removeItem(position);
        }
    }

    @Override
    public boolean onItemChildLongClick(ViewGroup parent, View childView, int position) {
        if (childView.getId() == R.id.tv_item_normal_delete) {
            showToast("长按了删除 " + mAdapter.getItem(position).title);
            return true;
        }
        return false;
    }

    @Override
    public void onRVItemClick(ViewGroup parent, View itemView, int position) {
        showToast("点击了条目 " + mAdapter.getItem(position).title);
    }

    @Override
    public boolean onRVItemLongClick(ViewGroup parent, View itemView, int position) {
        showToast("长按了条目 " + mAdapter.getItem(position).title);
        return true;
    }
}