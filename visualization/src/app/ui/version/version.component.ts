import { Component, inject, OnInit } from '@angular/core';
import { VersionService } from "./version.service";

@Component({
  selector: 'app-version',
  imports: [],
  templateUrl: './version.component.html',
  standalone: true,
  styleUrl: './version.component.css'
})

export class VersionComponent implements OnInit {
  version: string = 'undefined'
  private versionService: VersionService = inject(VersionService)

  async ngOnInit(): Promise<void> {
    try {
      this.version = await this.versionService.getVersion()
    } catch (error) {
      console.error('Error fetching version:', error)
    }
  }
}
