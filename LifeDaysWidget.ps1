Add-Type -AssemblyName PresentationFramework
Add-Type -AssemblyName PresentationCore
Add-Type -AssemblyName WindowsBase

function T([int[]]$Codes) {
    return -join ($Codes | ForEach-Object { [char]$_ })
}

$appName = 'LifeDaysWidget'
$appData = Join-Path $env:APPDATA $appName
$configPath = Join-Path $appData 'birthday.txt'
$createdNew = $false
$mutex = New-Object Threading.Mutex($true, 'Local\LifeDaysWidget.SingleInstance', [ref]$createdNew)
if (-not $createdNew) {
    $mutex.Dispose()
    return
}
try {
$script:savedBirthday = $null

if (-not (Test-Path -LiteralPath $appData)) {
    New-Item -ItemType Directory -Path $appData | Out-Null
}

[xml]$xaml = @'
<Window xmlns="http://schemas.microsoft.com/winfx/2006/xaml/presentation"
        xmlns:x="http://schemas.microsoft.com/winfx/2006/xaml"
        Width="320" Height="246"
        WindowStartupLocation="CenterScreen"
        ResizeMode="NoResize"
        WindowStyle="None"
        AllowsTransparency="True"
        Background="Transparent"
        Topmost="True"
        ShowInTaskbar="True"
        FontFamily="Segoe UI, Microsoft YaHei UI">
  <Window.Resources>
    <LinearGradientBrush x:Key="PanelBrush" StartPoint="0,0" EndPoint="1,1">
      <GradientStop Color="#FFFEFA" Offset="0" />
      <GradientStop Color="#EEF5EA" Offset="0.72" />
      <GradientStop Color="#E3ECDD" Offset="1" />
    </LinearGradientBrush>
    <LinearGradientBrush x:Key="NumberBrush" StartPoint="0,0" EndPoint="1,1">
      <GradientStop Color="#10150F" Offset="0" />
      <GradientStop Color="#3F6037" Offset="1" />
    </LinearGradientBrush>
    <Style x:Key="IconButton" TargetType="Button">
      <Setter Property="Width" Value="26" />
      <Setter Property="Height" Value="26" />
      <Setter Property="Foreground" Value="#75816F" />
      <Setter Property="Background" Value="Transparent" />
      <Setter Property="BorderBrush" Value="Transparent" />
      <Setter Property="BorderThickness" Value="0" />
      <Setter Property="Cursor" Value="Hand" />
      <Setter Property="FontSize" Value="13" />
      <Setter Property="Template">
        <Setter.Value>
          <ControlTemplate TargetType="Button">
            <Border x:Name="Chrome" CornerRadius="13" Background="{TemplateBinding Background}">
              <ContentPresenter HorizontalAlignment="Center" VerticalAlignment="Center" />
            </Border>
            <ControlTemplate.Triggers>
              <Trigger Property="IsMouseOver" Value="True">
                <Setter TargetName="Chrome" Property="Background" Value="#E7EEDF" />
                <Setter Property="Foreground" Value="#27301F" />
              </Trigger>
              <Trigger Property="IsPressed" Value="True">
                <Setter TargetName="Chrome" Property="Background" Value="#D8E4CF" />
              </Trigger>
            </ControlTemplate.Triggers>
          </ControlTemplate>
        </Setter.Value>
      </Setter>
    </Style>
    <Style x:Key="MiniButton" TargetType="Button" BasedOn="{StaticResource IconButton}">
      <Setter Property="Width" Value="30" />
      <Setter Property="Height" Value="24" />
      <Setter Property="FontSize" Value="14" />
      <Setter Property="Foreground" Value="#7E8A77" />
    </Style>
    <Style x:Key="PillButton" TargetType="Button">
      <Setter Property="MinWidth" Value="68" />
      <Setter Property="Height" Value="30" />
      <Setter Property="Padding" Value="12,0" />
      <Setter Property="Foreground" Value="#43523A" />
      <Setter Property="Background" Value="#EDF3E7" />
      <Setter Property="BorderBrush" Value="#DDE8D5" />
      <Setter Property="BorderThickness" Value="1" />
      <Setter Property="Cursor" Value="Hand" />
      <Setter Property="FontSize" Value="12" />
      <Setter Property="FontWeight" Value="SemiBold" />
      <Setter Property="Template">
        <Setter.Value>
          <ControlTemplate TargetType="Button">
            <Border x:Name="Chrome" CornerRadius="15" Background="{TemplateBinding Background}" BorderBrush="{TemplateBinding BorderBrush}" BorderThickness="{TemplateBinding BorderThickness}">
              <ContentPresenter HorizontalAlignment="Center" VerticalAlignment="Center" Margin="{TemplateBinding Padding}" />
            </Border>
            <ControlTemplate.Triggers>
              <Trigger Property="IsMouseOver" Value="True">
                <Setter TargetName="Chrome" Property="Background" Value="#E3EDDB" />
                <Setter TargetName="Chrome" Property="BorderBrush" Value="#C9DABB" />
              </Trigger>
              <Trigger Property="IsPressed" Value="True">
                <Setter TargetName="Chrome" Property="Background" Value="#D5E4C9" />
              </Trigger>
            </ControlTemplate.Triggers>
          </ControlTemplate>
        </Setter.Value>
      </Setter>
    </Style>
    <Style x:Key="SoftCombo" TargetType="ComboBox">
      <Setter Property="Height" Value="34" />
      <Setter Property="Padding" Value="10,0" />
      <Setter Property="Foreground" Value="#2D3628" />
      <Setter Property="Background" Value="#F6F9F2" />
      <Setter Property="BorderBrush" Value="#DDE8D5" />
      <Setter Property="BorderThickness" Value="1" />
      <Setter Property="FontSize" Value="12" />
      <Setter Property="SnapsToDevicePixels" Value="True" />
      <Setter Property="ItemContainerStyle">
        <Setter.Value>
          <Style TargetType="ComboBoxItem">
            <Setter Property="MinHeight" Value="28" />
            <Setter Property="Padding" Value="10,4" />
            <Setter Property="HorizontalContentAlignment" Value="Center" />
            <Setter Property="Foreground" Value="#2D3628" />
            <Setter Property="Template">
              <Setter.Value>
                <ControlTemplate TargetType="ComboBoxItem">
                  <Border x:Name="ItemChrome" CornerRadius="10" Background="Transparent" Padding="{TemplateBinding Padding}">
                    <ContentPresenter HorizontalAlignment="{TemplateBinding HorizontalContentAlignment}" VerticalAlignment="Center" />
                  </Border>
                  <ControlTemplate.Triggers>
                    <Trigger Property="IsHighlighted" Value="True">
                      <Setter TargetName="ItemChrome" Property="Background" Value="#EEF5E8" />
                    </Trigger>
                    <Trigger Property="IsSelected" Value="True">
                      <Setter TargetName="ItemChrome" Property="Background" Value="#DCEAD2" />
                      <Setter Property="FontWeight" Value="SemiBold" />
                    </Trigger>
                  </ControlTemplate.Triggers>
                </ControlTemplate>
              </Setter.Value>
            </Setter>
          </Style>
        </Setter.Value>
      </Setter>
      <Setter Property="Template">
        <Setter.Value>
          <ControlTemplate TargetType="ComboBox">
            <Grid>
              <ToggleButton x:Name="ToggleButton" IsChecked="{Binding IsDropDownOpen, Mode=TwoWay, RelativeSource={RelativeSource TemplatedParent}}" Focusable="False" ClickMode="Press" Background="Transparent" BorderThickness="0">
                <Border x:Name="Chrome" CornerRadius="14" Background="{TemplateBinding Background}" BorderBrush="{TemplateBinding BorderBrush}" BorderThickness="{TemplateBinding BorderThickness}">
                  <Grid Margin="10,0,8,0">
                    <Grid.ColumnDefinitions>
                      <ColumnDefinition Width="*" />
                      <ColumnDefinition Width="Auto" />
                    </Grid.ColumnDefinitions>
                    <ContentPresenter Grid.Column="0" Content="{TemplateBinding SelectionBoxItem}" VerticalAlignment="Center" HorizontalAlignment="Center" />
                    <Path Grid.Column="1" Data="M 0 0 L 4 4 L 8 0 Z" Fill="#7C8972" Width="8" Height="5" Margin="6,0,0,0" Stretch="Fill" VerticalAlignment="Center" />
                  </Grid>
                </Border>
              </ToggleButton>
              <Popup x:Name="PART_Popup" Placement="Bottom" IsOpen="{TemplateBinding IsDropDownOpen}" AllowsTransparency="True" Focusable="False" PopupAnimation="Fade">
                <Border Margin="0,5,0,0" CornerRadius="14" Background="#FFFFFF" BorderBrush="#DDE8D5" BorderThickness="1" Padding="4" MaxHeight="220">
                  <Border.Effect>
                    <DropShadowEffect BlurRadius="18" ShadowDepth="6" Opacity="0.12" Color="#172114" />
                  </Border.Effect>
                  <ScrollViewer CanContentScroll="True">
                    <ItemsPresenter />
                  </ScrollViewer>
                </Border>
              </Popup>
            </Grid>
            <ControlTemplate.Triggers>
              <Trigger Property="IsMouseOver" Value="True">
                <Setter TargetName="Chrome" Property="BorderBrush" Value="#C7D9BB" />
                <Setter TargetName="Chrome" Property="Background" Value="#F1F6EC" />
              </Trigger>
              <Trigger Property="IsKeyboardFocusWithin" Value="True">
                <Setter TargetName="Chrome" Property="BorderBrush" Value="#AFC99E" />
              </Trigger>
            </ControlTemplate.Triggers>
          </ControlTemplate>
        </Setter.Value>
      </Setter>
    </Style>
  </Window.Resources>

  <Border CornerRadius="28" Background="{StaticResource PanelBrush}" BorderBrush="#DFE9D8" BorderThickness="1" Padding="18">
    <Border.Effect>
      <DropShadowEffect BlurRadius="38" ShadowDepth="11" Opacity="0.18" Color="#172114" />
    </Border.Effect>
    <Grid>
      <Grid.RowDefinitions>
        <RowDefinition Height="Auto" />
        <RowDefinition Height="*" />
        <RowDefinition Height="Auto" />
      </Grid.RowDefinitions>

      <DockPanel x:Name="DragHandle" Grid.Row="0" LastChildFill="True" Height="28">
        <Button x:Name="CloseButton" Style="{StaticResource IconButton}" DockPanel.Dock="Right" Content="x" />
        <StackPanel Orientation="Horizontal" VerticalAlignment="Center">
          <Border Width="7" Height="7" CornerRadius="4" Background="#7B9569" Margin="0,0,9,0" />
          <TextBlock x:Name="TitleText" FontSize="12" FontWeight="SemiBold" Foreground="#323B2A" VerticalAlignment="Center" />
        </StackPanel>
      </DockPanel>

      <Grid Grid.Row="1" Margin="0,8,0,8">
        <Grid.RowDefinitions>
          <RowDefinition Height="Auto" />
          <RowDefinition Height="*" />
          <RowDefinition Height="Auto" />
        </Grid.RowDefinitions>

        <TextBlock x:Name="HintText" Grid.Row="0" FontSize="12" Foreground="#7C8774" HorizontalAlignment="Center" />

        <Grid Grid.Row="1" VerticalAlignment="Center">
          <TextBlock x:Name="DaysText" Text="--" FontSize="74" FontWeight="SemiBold"
                     Foreground="{StaticResource NumberBrush}" HorizontalAlignment="Center" VerticalAlignment="Center"
                     LineHeight="76" TextOptions.TextFormattingMode="Display" />
        </Grid>

        <StackPanel Grid.Row="2" Orientation="Horizontal" HorizontalAlignment="Center">
          <Border CornerRadius="999" Background="#E7EFE0" Padding="13,5" Margin="0,0,6,0">
            <TextBlock x:Name="UnitText" FontSize="12" FontWeight="SemiBold" Foreground="#526748" />
          </Border>
          <Button x:Name="EditButton" Style="{StaticResource MiniButton}" Content="..." />
        </StackPanel>
      </Grid>

      <Border x:Name="EditPanel" Grid.Row="2" CornerRadius="20" Background="#FFFFFF" BorderBrush="#E2EBDD" BorderThickness="1" Padding="12" Visibility="Collapsed">
        <Grid>
          <Grid.RowDefinitions>
            <RowDefinition Height="Auto" />
            <RowDefinition Height="Auto" />
          </Grid.RowDefinitions>
          <Grid Grid.Row="0" Margin="0,0,0,10">
            <Grid.ColumnDefinitions>
              <ColumnDefinition Width="1.3*" />
              <ColumnDefinition Width="8" />
              <ColumnDefinition Width="1*" />
              <ColumnDefinition Width="8" />
              <ColumnDefinition Width="1*" />
            </Grid.ColumnDefinitions>
            <ComboBox x:Name="YearCombo" Grid.Column="0" Style="{StaticResource SoftCombo}" />
            <ComboBox x:Name="MonthCombo" Grid.Column="2" Style="{StaticResource SoftCombo}" />
            <ComboBox x:Name="DayCombo" Grid.Column="4" Style="{StaticResource SoftCombo}" />
          </Grid>
          <StackPanel Grid.Row="1" Orientation="Horizontal" HorizontalAlignment="Right">
            <Button x:Name="CancelButton" Style="{StaticResource PillButton}" Margin="0,0,8,0" />
            <Button x:Name="SaveButton" Style="{StaticResource PillButton}" Background="#DCE9D2" BorderBrush="#C8DABA" Foreground="#2F4328" />
          </StackPanel>
        </Grid>
      </Border>
    </Grid>
  </Border>
</Window>
'@

$reader = New-Object System.Xml.XmlNodeReader $xaml
$window = [Windows.Markup.XamlReader]::Load($reader)

$titleText = $window.FindName('TitleText')
$hintText = $window.FindName('HintText')
$daysText = $window.FindName('DaysText')
$unitText = $window.FindName('UnitText')
$yearCombo = $window.FindName('YearCombo')
$monthCombo = $window.FindName('MonthCombo')
$dayCombo = $window.FindName('DayCombo')
$editButton = $window.FindName('EditButton')
$editPanel = $window.FindName('EditPanel')
$saveButton = $window.FindName('SaveButton')
$cancelButton = $window.FindName('CancelButton')
$closeButton = $window.FindName('CloseButton')
$dragHandle = $window.FindName('DragHandle')

$titleText.Text = T @(24050,27963,22810,20037)
$hintText.Text = T @(20808,35774,32622,29983,26085)
$unitText.Text = T @(22825)
$saveButton.Content = T @(20445,23384)
$cancelButton.Content = T @(21462,28040)

$currentYear = [DateTime]::Today.Year
for ($year = $currentYear; $year -ge 1850; $year--) {
    [void]$yearCombo.Items.Add($year)
}
1..12 | ForEach-Object { [void]$monthCombo.Items.Add($_.ToString('00')) }

function Set-DayItems([int]$year, [int]$month, [int]$preferredDay) {
    $dayCombo.Items.Clear()
    if ($year -lt 1 -or $month -lt 1) { return }

    $maxDay = [DateTime]::DaysInMonth($year, $month)
    1..$maxDay | ForEach-Object { [void]$dayCombo.Items.Add($_.ToString('00')) }
    $day = [Math]::Min([Math]::Max($preferredDay, 1), $maxDay)
    $dayCombo.SelectedItem = $day.ToString('00')
}

function Sync-DayItems {
    if ($null -eq $yearCombo.SelectedItem -or $null -eq $monthCombo.SelectedItem) { return }
    $preferredDay = 1
    if ($null -ne $dayCombo.SelectedItem) {
        $preferredDay = [int]$dayCombo.SelectedItem
    }
    Set-DayItems ([int]$yearCombo.SelectedItem) ([int]$monthCombo.SelectedItem) $preferredDay
}

function Set-EditorDate($date) {
    $target = if ($null -ne $date) { $date.Date } else { [DateTime]::Today }
    $yearCombo.SelectedItem = $target.Year
    $monthCombo.SelectedItem = $target.Month.ToString('00')
    Set-DayItems $target.Year $target.Month $target.Day
}

function Get-EditorDate {
    if ($null -eq $yearCombo.SelectedItem -or $null -eq $monthCombo.SelectedItem -or $null -eq $dayCombo.SelectedItem) {
        return $null
    }

    $year = [int]$yearCombo.SelectedItem
    $month = [int]$monthCombo.SelectedItem
    $day = [int]$dayCombo.SelectedItem
    try {
        return [DateTime]::new($year, $month, $day)
    }
    catch {
        return $null
    }
}

function Is-ValidBirthday($date) {
    if ($null -eq $date) { return $false }
    $minBirthday = [DateTime]::new(1850, 1, 1)
    return $date.Date -ge $minBirthday -and $date.Date -le [DateTime]::Today
}

function Save-Birthday($date) {
    if (Is-ValidBirthday $date) {
        $date.ToString('yyyy-MM-dd') | Set-Content -LiteralPath $configPath -Encoding ASCII
        $script:savedBirthday = $date.Date
    }
}

function Show-Editor([bool]$visible) {
    if ($visible) {
        $editPanel.Visibility = 'Visible'
        $editButton.Visibility = 'Collapsed'
        $window.Height = 296
        $hintText.Text = T @(36873,25321,20320,30340,29983,26085)
    }
    else {
        $editPanel.Visibility = 'Collapsed'
        $editButton.Visibility = 'Visible'
        $window.Height = 246
    }
}

function Update-Days {
    if ($null -eq $script:savedBirthday) {
        $daysText.Text = '--'
        $hintText.Text = T @(20808,35774,32622,29983,26085)
        return
    }

    $birthday = $script:savedBirthday.Date
    $today = [DateTime]::Today
    if (-not (Is-ValidBirthday $birthday)) {
        $daysText.Text = '--'
        $hintText.Text = T @(29983,26085,19981,33021,26202,20110,20170,22825)
        return
    }

    $days = [int]($today - $birthday).TotalDays
    $daysText.Text = $days.ToString('N0')
    $hintText.Text = T @(20320,24050,32463,27963,20102)
}

if (Test-Path -LiteralPath $configPath) {
    $rawBirthday = (Get-Content -LiteralPath $configPath -Raw).Trim()
    $parsedBirthday = [DateTime]::MinValue
    if ([DateTime]::TryParseExact($rawBirthday, 'yyyy-MM-dd', $null, [Globalization.DateTimeStyles]::None, [ref]$parsedBirthday) -and (Is-ValidBirthday $parsedBirthday)) {
        $script:savedBirthday = $parsedBirthday.Date
    }
}

Set-EditorDate $script:savedBirthday
$yearCombo.Add_SelectionChanged({ Sync-DayItems })
$monthCombo.Add_SelectionChanged({ Sync-DayItems })

$editButton.Add_Click({
    Set-EditorDate $script:savedBirthday
    Show-Editor $true
})

$saveButton.Add_Click({
    $selected = Get-EditorDate
    if ($null -eq $selected) {
        $hintText.Text = T @(35831,20808,36873,25321,29983,26085)
        return
    }
    if (-not (Is-ValidBirthday $selected)) {
        $hintText.Text = T @(29983,26085,19981,33021,26202,20110,20170,22825)
        return
    }

    Save-Birthday $selected.Date
    Update-Days
    Show-Editor $false
})

$cancelButton.Add_Click({
    Set-EditorDate $script:savedBirthday
    Update-Days
    if ($null -ne $script:savedBirthday) {
        Show-Editor $false
    }
})

$closeButton.Add_Click({ $window.Close() })
$dragHandle.Add_MouseLeftButtonDown({
    if ($_.ChangedButton -eq [Windows.Input.MouseButton]::Left) {
        try { $window.DragMove() } catch { }
    }
})

$timer = New-Object Windows.Threading.DispatcherTimer
$timer.Interval = [TimeSpan]::FromMinutes(1)
$timer.Add_Tick({ Update-Days })
$timer.Start()

Update-Days
if ($null -eq $script:savedBirthday) {
    Show-Editor $true
}
else {
    Show-Editor $false
}

[void]$window.ShowDialog()
}
finally {
    if ($null -ne $mutex) {
        $mutex.ReleaseMutex()
        $mutex.Dispose()
    }
}
