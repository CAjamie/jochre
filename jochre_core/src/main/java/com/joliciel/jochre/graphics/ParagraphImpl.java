package com.joliciel.jochre.graphics;

import java.util.ArrayList;
import java.util.List;

import com.joliciel.jochre.JochreSession;

public class ParagraphImpl implements ParagraphInternal {
	private int id;
	List<RowOfShapes> rows;
	private GraphicsServiceInternal graphicsService;

	private int index;
	private int imageId;
	private JochreImage image = null;

	private boolean coordinatesFound = false;
	private int left;
	private int top;
	private int right;
	private int bottom;

	private Boolean junk = null;

	@Override
	public JochreImage getImage() {
		if (this.imageId != 0 && this.image == null) {
			this.image = this.graphicsService.loadJochreImage(this.imageId);
		}
		return this.image;
	}

	@Override
	public void setImage(JochreImage image) {
		this.image = image;
		if (image != null)
			this.setImageId(image.getId());
		else
			this.setImageId(0);
	}

	@Override
	public void save() {
		if (this.image != null && this.imageId == 0)
			this.imageId = this.image.getId();

		this.graphicsService.saveParagraph(this);
		if (this.rows != null) {
			int index = 0;
			for (RowOfShapes row : this.rows) {
				RowOfShapesInternal iRow = (RowOfShapesInternal) row;
				iRow.setParagraph(this);
				row.setIndex(index++);
				row.save();
			}
		}
	}

	@Override
	public int getIndex() {
		return index;
	}

	@Override
	public void setIndex(int index) {
		this.index = index;
	}

	@Override
	public int getImageId() {
		return imageId;
	}

	@Override
	public void setImageId(int imageId) {
		this.imageId = imageId;
	}

	@Override
	public List<RowOfShapes> getRows() {
		if (rows == null) {
			if (this.id == 0)
				rows = new ArrayList<RowOfShapes>();
			else {
				rows = graphicsService.findRows(this);
				for (RowOfShapes row : rows) {
					((RowOfShapesInternal) row).setParagraph(this);
				}
			}
		}
		return rows;
	}

	@Override
	public RowOfShapes newRow() {
		RowOfShapesInternal row = graphicsService.getEmptyRowOfShapesInternal();
		row.setParagraph(this);
		this.getRows().add(row);
		return row;
	}

	public GraphicsServiceInternal getGraphicsService() {
		return graphicsService;
	}

	public void setGraphicsService(GraphicsServiceInternal graphicsService) {
		this.graphicsService = graphicsService;
	}

	@Override
	public int getLeft() {
		this.findCoordinates();
		return this.left;
	}

	@Override
	public int getTop() {
		this.findCoordinates();
		return this.top;
	}

	@Override
	public int getRight() {
		this.findCoordinates();
		return this.right;
	}

	@Override
	public int getBottom() {
		this.findCoordinates();
		return this.bottom;
	}

	private void findCoordinates() {
		if (!coordinatesFound) {
			RowOfShapes firstRow = this.getRows().iterator().next();
			left = firstRow.getLeft();
			top = firstRow.getTop();
			right = firstRow.getRight();
			bottom = firstRow.getBottom();

			for (RowOfShapes row : this.getRows()) {
				if (row.getLeft() < left)
					left = row.getLeft();
				if (row.getTop() < top)
					top = row.getTop();
				if (row.getRight() > right)
					right = row.getRight();
				if (row.getBottom() > bottom)
					bottom = row.getBottom();
			}
			coordinatesFound = true;
		}
	}

	@Override
	public boolean isJunk() {
		if (junk == null) {
			if (this.getRows().size() > 0) {
				double averageConfidence = 0;
				double shapeCount = 0;
				for (RowOfShapes row : this.getRows()) {
					for (GroupOfShapes group : row.getGroups()) {
						if (group.getShapes().size() > 0) {
							for (Shape shape : group.getShapes()) {
								averageConfidence += shape.getConfidence();
								shapeCount += 1;
							}
						}
					}
				}
				averageConfidence = averageConfidence / shapeCount;

				JochreSession jochreSession = JochreSession.getInstance();
				if (averageConfidence < jochreSession.getJunkConfidenceThreshold())
					junk = true;
				else
					junk = false;
			} else {
				junk = true;
			}
		}
		return junk;
	}

	@Override
	public String toString() {
		return "Par " + this.getIndex() + ", left(" + this.getLeft() + ")" + ", top(" + this.getTop() + ")" + ", right(" + this.getRight() + ")" + ", bot("
				+ this.getBottom() + ")" + ", width(" + this.getWidth() + ")" + ", height(" + this.getHeight() + ")";
	}

	@Override
	public int getWidth() {
		return right - left + 1;
	}

	@Override
	public int getHeight() {
		return bottom - top + 1;
	}

	@Override
	public void addRow(RowOfShapes row) {
		this.getRows().add(row);
		((RowOfShapesInternal) row).setParagraph(this);
	}

	@Override
	public int getId() {
		return id;
	}

	@Override
	public void setId(int id) {
		this.id = id;
	}
}
