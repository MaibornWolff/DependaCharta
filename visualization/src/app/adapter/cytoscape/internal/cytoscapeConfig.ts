import {StylesheetStyle} from 'cytoscape';

/*
* For most of them the default are fine: https://js.cytoscape.org/#init-opts/
* */
export const cytoscape_options = {
  wheelSensitivity: 5.0,
  // data: {...} could be useful for extra info
  // selectionType: 'additive' | 'single -> if the selection of a node should be added to a set of selections or if one node at a time should be selected (default is 'single')
}

/*
* Other available selectors: https://js.cytoscape.org/#selectors
* */
export const cytoscape_style: StylesheetStyle[] =
  [
    {
      selector: '.non-compound',
      style: {
        'width': '98px',
        'height': '48px',
        'border-width': 0,
        'shape': 'rectangle'
      }
    },
    {
      selector: '.compound',
      style: {
        'padding': '30px',
        'width': '98px',
        'height': '48px',
        'min-width': '98px',
        'min-height': '48px',
        'border-width': 0,
        'shape': 'rectangle'
      }
    },
    {
      selector: '.no-overlay',
      style: {
        'overlay-opacity': 0
      }
    },
    {
      selector: 'edge',
      style: {
        'width': 1,
        'line-color': 'grey',
        'target-arrow-color': 'grey',
        'target-arrow-shape': 'triangle-backcurve',
        'curve-style': 'bezier',
        'arrow-scale': 1.2,
        'z-index':8,
      }
    },
    // only loop edges (source = target)
    {
      selector: ':loop',
      style: {
        'width': 0.5,
        'line-color': 'grey',
        'target-arrow-color': 'grey',
        'loop-direction': '0',
        'arrow-scale': 1.2,
      }
    }
  ]
