package `in`.qwicklabs.vanam

import android.os.Bundle
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.WindowCompat
import androidx.fragment.app.Fragment
import `in`.qwicklabs.vanam.databinding.ActivityDashboardBinding
import `in`.qwicklabs.vanam.fragments.Community
import `in`.qwicklabs.vanam.fragments.Home
import `in`.qwicklabs.vanam.fragments.Map
import `in`.qwicklabs.vanam.fragments.Profile

class Dashboard : AppCompatActivity() {
    private lateinit var binding: ActivityDashboardBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        binding = ActivityDashboardBinding.inflate(layoutInflater)
        setContentView(binding.root)

        WindowCompat.getInsetsController(window, window.decorView).isAppearanceLightStatusBars =
            true
        WindowCompat.getInsetsController(window, window.decorView).isAppearanceLightNavigationBars =
            true

        window.navigationBarColor = getColor(R.color.dashboardBg)

        changeFragment(Home())

        binding.bottomNavigationView.setOnItemSelectedListener {
            when (it.itemId) {
                R.id.nav_home -> changeFragment(Home())
                R.id.nav_map -> changeFragment(Map())
                R.id.nav_community -> changeFragment(Community())
                R.id.nav_profile -> changeFragment(Profile())
            }
            true
        }
    }

    private fun changeFragment(fragment: Fragment) {
        supportFragmentManager.beginTransaction().replace(binding.fragmentContainer.id, fragment)
            .commit()
    }
}