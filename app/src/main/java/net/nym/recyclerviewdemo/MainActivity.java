package net.nym.recyclerviewdemo;

import android.content.Context;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageManager;
import android.graphics.drawable.Drawable;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.support.v7.widget.GridLayoutManager;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;
import android.widget.ImageView;
import android.widget.TextView;

import net.nym.recyclerviewlibrary.adapter.BaseRecyclerAdapter;
import net.nym.recyclerviewlibrary.animator.SimpleAnimator;
import net.nym.recyclerviewlibrary.divider.RecyclerViewGridDivider;
import net.nym.recyclerviewlibrary.endless.EndlessRecyclerOnScrollListener;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Locale;
import java.util.Random;

import static net.nym.recyclerviewdemo.R.id.parent;

public class MainActivity extends AppCompatActivity {
    public static final int FILTER_ALL_APP = 0; // 所有应用程序
    public static final int FILTER_SYSTEM_APP = 1; // 系统程序
    public static final int FILTER_THIRD_APP = 2; // 第三方应用程序
    public static final int FILTER_SDCARD_APP = 3; // 安装在SDCard的应用程序

    private RecyclerView recyclerView;
    private List<AppInfo> mData;
    private MBaseRecyclerAdapter mAdapter;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        recyclerView = (RecyclerView) findViewById(R.id.recyclerView);
        mData = new ArrayList<>();
        mAdapter = new MBaseRecyclerAdapter(this,mData);
        mAdapter.setOnItemClickListener(new BaseRecyclerAdapter.OnItemClickListener() {
            Random random = new Random();
            @Override
            public void onItemClick(View view, int position, long id) {
                if (random.nextInt(2) == 0){
                    mData.remove(position);
                    mAdapter.notifyItemRemoved(position);
                }else {
                    List<AppInfo> list = queryFilterAppInfo(FILTER_THIRD_APP);
                    mData.add(position,list.get(0));
                    mAdapter.notifyItemInserted(position);
                }
            }
        });
//        GridLayoutManager gridLayoutManager = new GridLayoutManager(this,3, LinearLayoutManager.VERTICAL,false);
//        gridLayoutManager.setSpanSizeLookup(new GridLayoutManager.SpanSizeLookup() {
//            @Override
//            public int getSpanSize(int position) {
//                return 0;
//            }
//        });
        LinearLayoutManager linearLayoutManager = new LinearLayoutManager(this);
        recyclerView.setLayoutManager(linearLayoutManager);
        recyclerView.addItemDecoration(new RecyclerViewGridDivider(this));
        recyclerView.setItemAnimator(new SimpleAnimator());
        recyclerView.setAdapter(mAdapter);
        mData.addAll(queryFilterAppInfo(FILTER_SYSTEM_APP));
        mAdapter.notifyDataSetChanged();

        recyclerView.addOnScrollListener(new EndlessRecyclerOnScrollListener() {
            @Override
            public void onLoadNextPage(View view) {
                mData.addAll(queryFilterAppInfo(FILTER_THIRD_APP));
                mAdapter.notifyDataSetChanged();
                System.out.println("onLoadNextPage");
            }
        });

    }

    // 根据查询条件，查询特定的ApplicationInfo
    private List<AppInfo> queryFilterAppInfo(int filter) {
        PackageManager pm = this.getPackageManager();
        // 查询所有已经安装的应用程序
        List<ApplicationInfo> listAppcations = pm
                .getInstalledApplications(PackageManager.GET_UNINSTALLED_PACKAGES);
        Collections.sort(listAppcations,
                new ApplicationInfo.DisplayNameComparator(pm));// 排序
        List<AppInfo> appInfos = new ArrayList<AppInfo>(); // 保存过滤查到的AppInfo
        // 根据条件来过滤
        switch (filter) {
            case FILTER_ALL_APP: // 所有应用程序
                appInfos.clear();
                for (ApplicationInfo app : listAppcations) {
                    appInfos.add(getAppInfo(pm,app));
                }
                break;
            case FILTER_SYSTEM_APP: // 系统程序
                appInfos.clear();
                for (ApplicationInfo app : listAppcations) {
                    if ((app.flags & ApplicationInfo.FLAG_SYSTEM) != 0) {
                        appInfos.add(getAppInfo(pm,app));
                    }
                }
                break;
            case FILTER_THIRD_APP: // 第三方应用程序
                appInfos.clear();
                for (ApplicationInfo app : listAppcations) {
                    //非系统程序
                    if ((app.flags & ApplicationInfo.FLAG_SYSTEM) <= 0) {
                        appInfos.add(getAppInfo(pm,app));
                    }
                    //本来是系统程序，被用户手动更新后，该系统程序也成为第三方应用程序了
                    else if ((app.flags & ApplicationInfo.FLAG_UPDATED_SYSTEM_APP) != 0){
                        appInfos.add(getAppInfo(pm,app));
                    }
                }
                break;
            case FILTER_SDCARD_APP: // 安装在SDCard的应用程序
                appInfos.clear();
                for (ApplicationInfo app : listAppcations) {
                    if ((app.flags & ApplicationInfo.FLAG_EXTERNAL_STORAGE) != 0) {
                        appInfos.add(getAppInfo(pm,app));
                    }
                }
                break;
            default:
                break;
        }
        return appInfos;
    }

    private AppInfo getAppInfo(PackageManager pm,ApplicationInfo app) {
        AppInfo info = new AppInfo();
        info.setAppIcon(app.loadIcon(pm));
        info.setAppLabel((String) app.loadLabel(pm));
        info.setPackageName(app.packageName);
        return info;
    }

    private class MBaseRecyclerAdapter extends BaseRecyclerAdapter<MBaseRecyclerAdapter.MViewHolder,AppInfo> {
        private int lastPosition = -1;
        public MBaseRecyclerAdapter(Context context, List<AppInfo> data) {
            super(context, data);
        }

        @Override
        public RecyclerView.ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
            View view = LayoutInflater.from(mContext).inflate(R.layout.item,parent,false);
            return new MViewHolder(view);
        }

        @Override
        protected void bindData(MViewHolder holder, AppInfo item,int position) {
            holder.imageView.setImageDrawable(item.getAppIcon());
            holder.label.setText(String.format(Locale.getDefault(),"%d.%s",position + 1,item.getAppLabel()));
            holder.packageName.setText(item.getPackageName());

            /****滑动进入动画效果****/
            setAnimation(holder.itemView,position);
            /****滑动进入动画效果****/
        }

        /****滑动进入动画效果****/
        private void setAnimation(View viewToAnimate, int position) {
            if (position > lastPosition) {
                Animation animation = AnimationUtils.loadAnimation(viewToAnimate.getContext(), android.R
                        .anim.slide_in_left);
                viewToAnimate.startAnimation(animation);
                lastPosition = position;
            }
        }

        @Override
        public void onViewDetachedFromWindow(RecyclerView.ViewHolder holder) {
            super.onViewDetachedFromWindow(holder);
            holder.itemView.clearAnimation();
        }
        /****滑动进入动画效果****/

        class MViewHolder extends BaseRecyclerAdapter.ViewHolder {

            private ImageView imageView;
            private TextView label;
            private TextView packageName;
            public MViewHolder(View itemView) {
                super(itemView);
            }

            @Override
            protected void bindView(View itemView) {
                imageView = (ImageView) itemView.findViewById(R.id.imageView);
                label = (TextView) itemView.findViewById(R.id.label);
                packageName = (TextView) itemView.findViewById(R.id.packageName);
            }
        }
    };

    private class AppInfo{
        private String appLabel;
        private Drawable appIcon;
        private String packageName;

        public String getAppLabel() {
            return appLabel;
        }

        public void setAppLabel(String appLabel) {
            this.appLabel = appLabel;
        }

        public Drawable getAppIcon() {
            return appIcon;
        }

        public void setAppIcon(Drawable appIcon) {
            this.appIcon = appIcon;
        }

        public String getPackageName() {
            return packageName;
        }

        public void setPackageName(String packageName) {
            this.packageName = packageName;
        }
    }
}
