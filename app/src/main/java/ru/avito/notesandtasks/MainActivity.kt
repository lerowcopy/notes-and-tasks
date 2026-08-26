package ru.avito.notesandtasks

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            MaterialTheme {
                Surface {
                    HelloWorld()
                }
            }
        }
    }
}

@Composable
private fun HelloWorld() {
    Text(text = stringResource(R.string.hello_world))
}

@Preview(showBackground = true)
@Composable
private fun HelloWorldPreview() {
    MaterialTheme {
        HelloWorld()
    }
}
