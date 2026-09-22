package org.anjisuan608.hihonor.ai_freegrip

import android.os.Bundle
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import org.anjisuan608.hihonor.ai_freegrip.ui.AboutScreen

/**
 * 关于页 Activity——设置页的**子页**（standard 压栈，不参与底部导航）：
 *
 * - 顶栏返回箭头 + 系统返回键都走 finish()，回到栈中的设置页；
 * - 由 SettingsActivity 点入，或经 App Shortcuts 深链
 *   （MainActivity 先入设置页再压本页，返回链保持 关于→设置→主页）；
 * - `exported`：按需求四个界面 Activity 全部导出。
 */
class AboutActivity : BaseAppActivity() {

    @OptIn(ExperimentalMaterial3Api::class)
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        setContent {
            AppTheme {
                Scaffold(
                    topBar = {
                        TopAppBar(
                            title = { Text(stringResource(R.string.about_title)) },
                            navigationIcon = {
                                IconButton(onClick = { finish() }) {
                                    Icon(
                                        imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                                        contentDescription = stringResource(R.string.action_back),
                                    )
                                }
                            },
                        )
                    },
                ) { innerPadding ->
                    AboutScreen(modifier = Modifier.padding(innerPadding))
                }
            }
        }
    }
}
