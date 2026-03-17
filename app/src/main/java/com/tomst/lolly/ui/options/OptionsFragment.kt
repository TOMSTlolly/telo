package com.tomst.lolly.ui.options

import android.app.Activity
import android.app.DatePickerDialog
import android.content.Context
import android.content.Intent
import android.content.SharedPreferences
import android.content.pm.ActivityInfo
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.ui.platform.ComposeView
import androidx.compose.ui.platform.ViewCompositionStrategy
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
import androidx.navigation.fragment.findNavController
import com.google.firebase.auth.FirebaseAuth
import com.tomst.lolly.LoginActivity
import com.tomst.lolly.LollyActivity
import com.tomst.lolly.R
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale

class OptionsFragment : Fragment() {

    private lateinit var viewModel: OptionsViewModel
    private lateinit var openDocumentTreeLauncher: ActivityResultLauncher<Intent>
    private val dateFormatter = SimpleDateFormat("dd-MM-yyyy", Locale.getDefault())
    private val isoFormatter = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        viewModel = ViewModelProvider(this).get(OptionsViewModel::class.java)

        openDocumentTreeLauncher = registerForActivityResult(
            ActivityResultContracts.StartActivityForResult()
        ) { result ->
            if (result.resultCode == Activity.RESULT_OK) {
                val resultData = result.data
                val treeUri = resultData?.data
                if (treeUri != null) {
                    requireActivity().grantUriPermission(
                        requireActivity().packageName,
                        treeUri,
                        Intent.FLAG_GRANT_READ_URI_PERMISSION or Intent.FLAG_GRANT_WRITE_URI_PERMISSION
                    )
                    requireContext().contentResolver.takePersistableUriPermission(
                        treeUri,
                        Intent.FLAG_GRANT_READ_URI_PERMISSION or Intent.FLAG_GRANT_WRITE_URI_PERMISSION
                    )

                    setPrefExportFolder(treeUri.toString())
                    LollyActivity.getInstance().prefExportFolder = treeUri.toString()
                }
            }
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        readForm()
        updateUserInfo()

        return ComposeView(requireContext()).apply {
            setViewCompositionStrategy(ViewCompositionStrategy.DisposeOnViewTreeLifecycleDestroyed)
            setContent {
                OptionsScreen(
                    viewModel = viewModel,
                    onSaveClick = {
                        saveOptions()
                        Toast.makeText(context, "Options saved", Toast.LENGTH_SHORT).show()
                    },
                    onExportFolderClick = {
                        val intent = Intent(Intent.ACTION_OPEN_DOCUMENT_TREE).apply {
                            putExtra("android.content.extra.SHOW_ADVANCED", true)
                            putExtra("android.content.extra.FANCY", true)
                            flags = (Intent.FLAG_GRANT_READ_URI_PERMISSION
                                     or Intent.FLAG_GRANT_WRITE_URI_PERMISSION
                                     or Intent.FLAG_GRANT_PERSISTABLE_URI_PERMISSION)
                        }
                        openDocumentTreeLauncher.launch(intent)
                    },
                    onPickDateClick = {
                        showDatePicker()
                    },
                    onLoginClick = {
                        val intent = Intent(activity, LoginActivity::class.java)
                        startActivity(intent)
                        activity?.finish()
                    },
                    onLogoutClick = {
                        FirebaseAuth.getInstance().signOut()
                        val intent = Intent(activity, LoginActivity::class.java)
                        startActivity(intent)
                        activity?.finish()
                    },
                    onAboutClick = {
                        findNavController().navigate(R.id.navigation_about)
                    },
                    onNavigateBack = {
                        revertOptions()
                        findNavController().navigateUp()
                    }
                )
            }
        }
    }

    fun saveOptions() {
        saveForm()
        viewModel.markSaved()
        viewModel.setInitialState(viewModel.uiState.value)
    }

    fun revertOptions() {
        viewModel.markSaved() // Prevents BackHandler from blocking again
    }

    override fun onResume() {
        super.onResume()
        activity?.requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_PORTRAIT
    }

    override fun onPause() {
        super.onPause()
        // Save form on pause if needed, but the user wants a prompt.
        // For the bottom nav interception, we'd need more complex logic in Activity.
    }

    override fun onDestroyView() {
        // We don't auto-save anymore if user wants a prompt, 
        // but for safety in Android lifecycle we might still want to ensure data isn't lost.
        // However, the user specifically asked for a prompt when "leaving".
        synchronizeAppCache()
        super.onDestroyView()
    }

    private fun showDatePicker() {
        val c = Calendar.getInstance()
        val year = c.get(Calendar.YEAR)
        val month = c.get(Calendar.MONTH)
        val day = c.get(Calendar.DAY_OF_MONTH)

        DatePickerDialog(
            requireContext(),
            { _, y, m, d ->
                val dateStr = String.format(Locale.getDefault(), "%02d-%02d-%d", d, m + 1, y)
                try {
                    val date = dateFormatter.parse(dateStr)
                    if (date != null) {
                        viewModel.updateState { it.copy(fromDate = isoFormatter.format(date)) }
                    }
                } catch (e: Exception) {
                    Log.e("OptionsFragment", "Error parsing date", e)
                }
            },
            year, month, day
        ).show()
    }

    private fun saveForm() {
        val context = requireContext()
        val sharedPref = context.getSharedPreferences(getString(R.string.save_options), Context.MODE_PRIVATE)
        val state = viewModel.uiState.value

        with(sharedPref.edit()) {
            putInt("readFrom", state.readFrom)
            putString("commandBookmark", state.commandBookmark)
            putBoolean("checkboxBookmark", state.checkboxBookmark)
            putInt("mode", state.mode)
            putBoolean("showgraph", state.showGraph)
            putBoolean("rotategraph", state.rotateGraph)
            putBoolean("noledlight", state.noLedLight)
            putBoolean("showmicro", state.showMicro)
            putBoolean("settime", state.setTime)
            putString("decimalseparator", state.decimalSeparator)

            state.bookmarkVal.toIntOrNull()?.let {
                putInt("bookmarkVal", it)
            }
            putString("fromDate", state.fromDate)
            apply()
        }
    }

    private fun readForm() {
        val context = requireContext()
        val sharedPref = context.getSharedPreferences(getString(R.string.save_options), Context.MODE_PRIVATE)

        val folderUri = sharedPref.getString("prefExportFolder", "") ?: ""
        val folderName = extractFolderNameFromEncodedUri(folderUri)

        val loadedState = OptionsUiState(
            readFrom = sharedPref.getInt("readFrom", 0),
            commandBookmark = sharedPref.getString("commandBookmark", "") ?: "",
            checkboxBookmark = sharedPref.getBoolean("checkboxBookmark", false),
            mode = sharedPref.getInt("mode", 0),
            showGraph = sharedPref.getBoolean("showgraph", true),
            rotateGraph = sharedPref.getBoolean("rotategraph", true),
            noLedLight = sharedPref.getBoolean("noledlight", true),
            showMicro = sharedPref.getBoolean("showmicro", true),
            setTime = sharedPref.getBoolean("settime", true),
            decimalSeparator = sharedPref.getString("decimalseparator", ",") ?: ",",
            exportFolder = folderName,
            bookmarkVal = sharedPref.getInt("bookmarkVal", 0).let { v -> if (v == 0) "" else v.toString() },
            fromDate = sharedPref.getString("fromDate", "") ?: ""
        )
        
        viewModel.updateState { loadedState }
        viewModel.setInitialState(loadedState)
    }

    private fun updateUserInfo() {
        val user = FirebaseAuth.getInstance().currentUser
        viewModel.updateState { it.copy(userEmail = user?.email) }
    }

    private fun synchronizeAppCache() {
        val context = requireContext()
        val sharedPref = context.getSharedPreferences(getString(R.string.save_options), Context.MODE_PRIVATE)
        LollyActivity.getInstance().prefExportFolder = sharedPref.getString("prefExportFolder", "") ?: ""
    }

    private fun setPrefExportFolder(folder: String) {
        val context = requireContext()
        val sharedPref = context.getSharedPreferences(getString(R.string.save_options), Context.MODE_PRIVATE)
        sharedPref.edit().putString("prefExportFolder", folder).apply()

        val folderName = extractFolderNameFromEncodedUri(folder)
        viewModel.updateState { it.copy(exportFolder = folderName) }
    }

    private fun extractFolderNameFromEncodedUri(uriPath: String): String {
        val spath = Uri.decode(uriPath)
        val pathSeparator = ":"
        return if (spath.contains(pathSeparator)) {
            val spathParts = spath.split(pathSeparator)
            spathParts.last()
        } else spath
    }
}
