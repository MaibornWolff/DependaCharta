import { Injectable } from '@angular/core';

@Injectable({
  providedIn: 'root'
})

export class VersionService {
  private readonly jsonUrl = './version.json'

  async getVersion(): Promise<string> {
    try {
      const json = await this.fetchVersionJson()
      return json.version
    } catch (error) {
      console.error('Error getting version:', error)
      throw error
    }
  }

  private async fetchVersionJson(): Promise<any> {
    try {
      const response = await fetch(this.jsonUrl)
      return response.json()
    } catch (error) {
      console.log("Error fetching version json", error)
    }
  }
}
