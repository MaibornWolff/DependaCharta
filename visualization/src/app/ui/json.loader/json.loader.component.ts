import { Component, EventEmitter, OnInit, Output } from '@angular/core';
import { ProjectReport } from '../../adapter/analysis/internal/ProjectReport';

@Component({
  selector: 'json-loader',
  imports: [],
  templateUrl: './json.loader.component.html',
  standalone: true,
  styleUrl: './json.loader.component.css'
})
export class JsonLoaderComponent implements OnInit {
  @Output() fileLoadingStart = new EventEmitter<void>()
  @Output() loadedNewFile = new EventEmitter<ProjectReport>()
  defaultJsonPath = './resources/java-example.cg.json'
  // TODO remove leakage of test logic into production code
  // file exists only in git pipeline for cypress test
  analysisPath = './analysis/analyzed-project.cg.json'

  async ngOnInit(): Promise<void> {
    const urlParams = new URLSearchParams(window.location.search)
    const fileParam = urlParams.get('file')

    const filePath = fileParam ?? this.analysisPath
    console.log(`Loading ${filePath}`)

    try {
      const json = await this.fetchJson(filePath)
      this.loadedNewFile.emit(json)
    } catch (error) {
      console.error(`Error loading ${filePath}, trying default path:`, error)
      try {
        const defaultJson = await this.fetchJson(this.defaultJsonPath)
        this.loadedNewFile.emit(defaultJson)
      } catch (error) {
        console.error('Error loading default JSON:', error)
      }
    }
  }

  private async fetchJson(path: string): Promise<ProjectReport> {
    const response = await fetch(path)
    if (!response.ok) {
      const error = new Error(`HTTP error! status: ${response.status}`)
      console.log(`Error fetching json from ${path}:`, error)
      throw error
    }

    return response.json()
  }

  async loadNewFile(target?: EventTarget | null) {
    this.fileLoadingStart.emit()

    const file = (target as HTMLInputElement)?.files?.item(0)
    const filename = file?.name
    console.log(filename)
    const fileContent = await file?.text()
    const jsonData = (fileContent && JSON.parse(fileContent)) || await this.fetchJson(this.defaultJsonPath)

    this.loadedNewFile.emit(jsonData)
  }

  clearSelectedInput(target?: EventTarget | null) {
    if (target instanceof HTMLInputElement) {
      // sets the input value to null so the next loaded file will actually trigger the inputs change listener.
      // this allows for reloading a file with the same name.
      target.value = ''
    }
  }
}
