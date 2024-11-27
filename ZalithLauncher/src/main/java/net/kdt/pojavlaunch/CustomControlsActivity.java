package net.kdt.pojavlaunch;

import android.content.Intent;
import android.graphics.Color;
import android.net.Uri;
import android.os.Bundle;
import android.provider.DocumentsContract;
import android.view.View;
import android.widget.FrameLayout;

import androidx.activity.OnBackPressedCallback;
import androidx.drawerlayout.widget.DrawerLayout;

import com.movtery.zalithlauncher.R;
import com.movtery.zalithlauncher.context.ContextExecutor;
import com.movtery.zalithlauncher.databinding.ActivityCustomControlsBinding;
import com.movtery.zalithlauncher.databinding.ViewControlMenuBinding;
import com.movtery.zalithlauncher.feature.background.BackgroundManager;
import com.movtery.zalithlauncher.feature.background.BackgroundType;
import com.movtery.zalithlauncher.setting.AllSettings;
import com.movtery.zalithlauncher.ui.activity.BaseActivity;
import com.movtery.zalithlauncher.ui.dialog.ControlSettingsDialog;
import com.movtery.zalithlauncher.ui.subassembly.view.GameMenuViewWrapper;

import net.kdt.pojavlaunch.customcontrols.ControlData;
import net.kdt.pojavlaunch.customcontrols.ControlDrawerData;
import net.kdt.pojavlaunch.customcontrols.ControlJoystickData;
import net.kdt.pojavlaunch.customcontrols.ControlLayout;
import net.kdt.pojavlaunch.customcontrols.EditorExitable;

import java.io.IOException;

public class CustomControlsActivity extends BaseActivity implements EditorExitable {
	public static final String BUNDLE_CONTROL_PATH = "control_path";
	private ActivityCustomControlsBinding binding;
	private String mControlPath = null;

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		parseBundle();
		binding = ActivityCustomControlsBinding.inflate(getLayoutInflater());
		setContentView(binding.getRoot());

		ControlLayout controlLayout = binding.customctrlControllayout;
		DrawerLayout drawerLayout = binding.customctrlDrawerlayout;
		FrameLayout drawerNavigationView = binding.customctrlNavigationView;

		new GameMenuViewWrapper(this, v -> {
			boolean open = drawerLayout.isDrawerOpen(drawerNavigationView);

			if (open) drawerLayout.closeDrawer(drawerNavigationView);
			else drawerLayout.openDrawer(drawerNavigationView);
		}).setVisibility(true);

		BackgroundManager.setBackgroundImage(this, BackgroundType.CUSTOM_CONTROLS, binding.backgroundView);

		drawerLayout.setDrawerLockMode(DrawerLayout.LOCK_MODE_LOCKED_CLOSED);
		drawerLayout.setScrimColor(Color.TRANSPARENT);

		ViewControlMenuBinding controlMenuBinding = ViewControlMenuBinding.inflate(getLayoutInflater());
		new ControlSettingsClickListener(controlMenuBinding, controlLayout);

		drawerNavigationView.addView(controlMenuBinding.getRoot());
		controlLayout.setModifiable(true);
		try {
			if (mControlPath == null) controlLayout.loadLayout((String) null);
			else controlLayout.loadLayout(mControlPath);
		} catch (IOException e) {
			Tools.showError(this, e);
		}

		getOnBackPressedDispatcher().addCallback(this, new OnBackPressedCallback(true) {
			@Override
			public void handleOnBackPressed() {
				binding.customctrlControllayout.askToExit(CustomControlsActivity.this);
			}
		});
	}

	@Override
	protected void onResume() {
		super.onResume();
		ContextExecutor.setActivity(this);
	}

	private void parseBundle() {
		Bundle bundle = getIntent().getExtras();
		if (bundle != null) {
			mControlPath = bundle.getString(BUNDLE_CONTROL_PATH);
		}
	}

	@Override
	public boolean shouldIgnoreNotch() {
		return AllSettings.getIgnoreNotch();
	}

	@Override
	public void exitEditor() {
		finish();
	}

	private class ControlSettingsClickListener implements View.OnClickListener {
		private final ViewControlMenuBinding binding;
		private final ControlLayout controlLayout;

		public ControlSettingsClickListener(ViewControlMenuBinding binding, ControlLayout controlLayout) {
			this.binding = binding;
			this.controlLayout = controlLayout;
			this.binding.addButton.setOnClickListener(this);
			this.binding.addDrawer.setOnClickListener(this);
			this.binding.addJoystick.setOnClickListener(this);
			this.binding.controlsSettings.setOnClickListener(this);
			this.binding.load.setOnClickListener(this);
			this.binding.save.setOnClickListener(this);
			this.binding.saveAndExit.setOnClickListener(this);
			this.binding.selectDefault.setOnClickListener(this);
			this.binding.export.setOnClickListener(this);
			this.binding.exit.setOnClickListener(this);
		}

		@Override
		public void onClick(View v) {
			if (v == binding.addButton) controlLayout.addControlButton(new ControlData(getString(R.string.controls_add_control_button)));
			else if (v == binding.addDrawer) controlLayout.addDrawer(new ControlDrawerData());
			else if (v == binding.addJoystick) controlLayout.addJoystickButton(new ControlJoystickData());
			else if (v == binding.controlsSettings) new ControlSettingsDialog(CustomControlsActivity.this).show();
			else if (v == binding.load) controlLayout.openLoadDialog();
			else if (v == binding.save) controlLayout.openSaveDialog();
			else if (v == binding.saveAndExit) controlLayout.openSaveAndExitDialog(CustomControlsActivity.this);
			else if (v == binding.selectDefault) controlLayout.openSetDefaultDialog();
			else if (v == binding.export) {
				try { // Saving the currently shown control
					Uri contentUri = DocumentsContract.buildDocumentUri(getString(R.string.storageProviderAuthorities), controlLayout.saveToDirectory(controlLayout.mLayoutFileName));

					Intent shareIntent = new Intent();
					shareIntent.setAction(Intent.ACTION_SEND);
					shareIntent.putExtra(Intent.EXTRA_STREAM, contentUri);
					shareIntent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
					shareIntent.setType("application/json");
					startActivity(shareIntent);

					Intent sendIntent = Intent.createChooser(shareIntent, controlLayout.mLayoutFileName);
					startActivity(sendIntent);
				} catch (Exception e) {
					Tools.showError(CustomControlsActivity.this, e);
				}
			}
			else if (v == binding.exit) controlLayout.openExitDialog(CustomControlsActivity.this);
		}
	}
}
