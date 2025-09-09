package com.example.truthnudge

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.util.Log
import android.view.MenuItem
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.ActionBarDrawerToggle
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import androidx.drawerlayout.widget.DrawerLayout
import androidx.recyclerview.widget.ItemTouchHelper
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.truthnudge.data.AppDatabase
import com.example.truthnudge.data.Claim
import com.example.truthnudge.databinding.ActivityHomeBinding
import com.google.android.material.navigation.NavigationView
import com.google.android.material.snackbar.Snackbar
import com.google.firebase.auth.FirebaseAuth
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class HomeActivity : AppCompatActivity() {

    private lateinit var binding: ActivityHomeBinding
    private lateinit var auth: FirebaseAuth
    private lateinit var db: AppDatabase
    private lateinit var adapter: ClaimsAdapter
    private lateinit var drawerLayout: DrawerLayout
    private lateinit var navigationView: NavigationView
    private lateinit var toggle: ActionBarDrawerToggle
    private lateinit var recentClaimsRecycler: RecyclerView

    private val requestPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { isGranted: Boolean ->
        Log.d(
            "PERMISSION",
            if (isGranted) "Notification permission granted ✅" else "Notification permission denied ❌"
        )
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityHomeBinding.inflate(layoutInflater)
        setContentView(binding.root)

        auth = FirebaseAuth.getInstance()
        db = AppDatabase.getDatabase(this)

        drawerLayout = binding.drawerLayout
        navigationView = binding.navigationView

        // Toolbar & Drawer toggle
        setSupportActionBar(binding.topAppBar)
        supportActionBar?.setDisplayHomeAsUpEnabled(false)
        toggle = ActionBarDrawerToggle(
            this,
            drawerLayout,
            binding.topAppBar,
            R.string.drawer_open,
            R.string.drawer_close
        )
        drawerLayout.addDrawerListener(toggle)
        toggle.syncState()

        // Navigation Drawer menu click
        navigationView.setNavigationItemSelectedListener { menuItem ->
            when (menuItem.itemId) {
                R.id.nav_home -> drawerLayout.closeDrawers()
                R.id.nav_history -> startActivity(Intent(this, HistoryActivity::class.java))
                R.id.nav_logout -> {
                    auth.signOut()
                    startActivity(Intent(this, LoginActivity::class.java).apply {
                        flags =
                            Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
                    })
                    finish()
                }
                R.id.nav_settings -> openNotificationSettings()
            }
            drawerLayout.closeDrawers()
            true
        }

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU &&
            ContextCompat.checkSelfPermission(
                this,
                Manifest.permission.POST_NOTIFICATIONS
            ) != PackageManager.PERMISSION_GRANTED
        ) {
            requestPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
        }

        recentClaimsRecycler = binding.recentClaimsRecycler

        requestNotificationPermission()
        setupRecyclerView()
        setupCards()
        ensureNotificationAccess()
    }

    private fun requestNotificationPermission() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU &&
            ContextCompat.checkSelfPermission(
                this,
                Manifest.permission.POST_NOTIFICATIONS
            ) != PackageManager.PERMISSION_GRANTED
        ) {
            requestPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
        }
    }

    private fun ensureNotificationAccess() {
        val enabledPackages = NotificationManagerCompat.getEnabledListenerPackages(this)
        if (!enabledPackages.contains(packageName)) {
            startActivity(Intent("android.settings.ACTION_NOTIFICATION_LISTENER_SETTINGS"))
            Snackbar.make(
                binding.root,
                "Please enable TruthNudge in Notification Access",
                Snackbar.LENGTH_LONG
            ).show()
        }
    }

    private fun openNotificationSettings() {
        startActivity(Intent("android.settings.ACTION_NOTIFICATION_LISTENER_SETTINGS"))
    }

    private fun setupRecyclerView() {
        adapter = ClaimsAdapter(mutableListOf()) { claimText ->
            startActivity(Intent(this, ShareReceiverActivity::class.java).apply {
                putExtra(Intent.EXTRA_TEXT, claimText)
            })
        }
        binding.recentClaimsRecycler.layoutManager = LinearLayoutManager(this)
        binding.recentClaimsRecycler.adapter = adapter
        enableSwipeToDelete()
        loadRecentClaims()
    }

    private fun setupCards() {
        binding.checkClaimCard.apply {
            translationY = -300f
            alpha = 0f
            animate().translationY(0f).alpha(1f).setDuration(500).start()
            setOnClickListener {
                startActivity(
                    Intent(
                        this@HomeActivity,
                        ShareReceiverActivity::class.java
                    )
                )
            }
        }

        binding.historyCard.apply {
            translationY = -300f
            alpha = 0f
            animate().translationY(0f).alpha(1f).setDuration(600).start()
            setOnClickListener {
                startActivity(
                    Intent(
                        this@HomeActivity,
                        HistoryActivity::class.java
                    )
                )
            }
        }
    }

    override fun onStart() {
        super.onStart()
        if (auth.currentUser == null) {
            startActivity(Intent(this, LoginActivity::class.java).apply {
                flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
            })
            finish()
        }
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        if (toggle.onOptionsItemSelected(item)) return true
        return super.onOptionsItemSelected(item)
    }

    private fun loadRecentClaims() {
        CoroutineScope(Dispatchers.IO).launch {
            val recentClaims: List<Claim> = db.claimDao().getLastNClaims(5)
            runOnUiThread {
                adapter.updateClaims(recentClaims.map { "${it.text}\nVerdict: ${it.verdict}" })
            }
        }
    }

    private fun enableSwipeToDelete() {
        val callback =
            object : ItemTouchHelper.SimpleCallback(0, ItemTouchHelper.LEFT or ItemTouchHelper.RIGHT) {
                override fun onMove(
                    recyclerView: RecyclerView,
                    vh: RecyclerView.ViewHolder,
                    target: RecyclerView.ViewHolder
                ) = false

                override fun onSwiped(viewHolder: RecyclerView.ViewHolder, direction: Int) {
                    val position = viewHolder.adapterPosition
                    val deletedClaim = adapter.claimList[position]

                    CoroutineScope(Dispatchers.IO).launch {
                        val claim =
                            db.claimDao().getAllClaims().find { "${it.text}\nVerdict: ${it.verdict}" == deletedClaim }
                        claim?.let { db.claimDao().deleteClaim(it) }
                    }

                    val updatedList =
                        adapter.claimList.toMutableList().apply { removeAt(position) }
                    adapter.updateClaims(updatedList)

                    Snackbar.make(binding.recentClaimsRecycler, "Claim deleted", Snackbar.LENGTH_LONG)
                        .setAction("UNDO") {
                            CoroutineScope(Dispatchers.IO).launch {
                                val claim =
                                    db.claimDao().getAllClaims().find { "${it.text}\nVerdict: ${it.verdict}" == deletedClaim }
                                claim?.let { db.claimDao().insertClaim(it) }
                            }
                            adapter.updateClaims(
                                adapter.claimList.toMutableList().apply { add(position, deletedClaim) })
                        }.show()
                }
            }
        ItemTouchHelper(callback).attachToRecyclerView(binding.recentClaimsRecycler)
    }
}
